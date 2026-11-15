import { Injectable, NgZone } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';

export type LiveState = 'idle' | 'connecting' | 'live' | 'reconnecting' | 'offline';

interface Watcher { topic: string; next: (v: unknown) => void; sub?: StompSubscription; }

/**
 * One STOMP client for the whole app (plain WebSocket, no SockJS). Public endpoint: no JWT is sent.
 * Created on the first subscription, closed when the last subscriber leaves and when the user logs out.
 */
@Injectable({ providedIn: 'root' })
export class StompService {
  readonly state$ = new BehaviorSubject<LiveState>('idle');
  /** Fires when the tab becomes visible again after the connection was paused (consumers refresh via REST). */
  readonly resumed$ = new Subject<void>();
  /** Time of the last message, for stale-data detection. */
  lastMessageAt = 0;

  private client?: Client;
  private readonly watchers = new Set<Watcher>();
  private failures = 0;
  private everConnected = false;
  private hiddenTimer?: ReturnType<typeof setTimeout>;
  private paused = false;

  constructor(private zone: NgZone, auth: AuthService) {
    auth.currentUser$.subscribe(u => { if (!u) this.closeAll(); });
    document.addEventListener('visibilitychange', () => this.onVisibility());
  }

  /** `ws://localhost:8082/ws` derived from environment.apiUrl (`https` → `wss`). */
  private get brokerUrl(): string {
    const u = new URL(environment.apiUrl, window.location.origin);
    return `${u.protocol === 'https:' ? 'wss:' : 'ws:'}//${u.host}/ws`;
  }

  watch<T>(topic: string): Observable<T> {
    return new Observable<T>(subscriber => {
      const w: Watcher = { topic, next: v => subscriber.next(v as T) };
      this.watchers.add(w);
      this.ensureClient();
      if (this.client?.connected) this.subscribe(w);
      return () => {
        w.sub?.unsubscribe();
        this.watchers.delete(w);
        if (this.watchers.size === 0) this.shutdown();
      };
    });
  }

  private subscribe(w: Watcher): void {
    if (!this.client?.connected || w.sub) return;
    w.sub = this.client.subscribe(w.topic, (m: IMessage) => {
      this.lastMessageAt = Date.now();
      try {
        const parsed = JSON.parse(m.body);
        this.zone.run(() => w.next(parsed));
      } catch {
        /* ignore malformed frames */
      }
    });
  }

  private ensureClient(): void {
    if (this.client || this.paused) return;
    this.state$.next(this.everConnected ? 'reconnecting' : 'connecting');
    const client = new Client({
      brokerURL: this.brokerUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });
    client.onConnect = () => this.zone.run(() => {
      this.failures = 0;
      this.everConnected = true;
      client.reconnectDelay = 5000;
      this.watchers.forEach(w => { w.sub = undefined; this.subscribe(w); });
      this.state$.next('live');
    });
    client.onWebSocketClose = () => this.zone.run(() => {
      this.watchers.forEach(w => (w.sub = undefined));
      if (!this.client) return;
      this.failures++;
      client.reconnectDelay = Math.min(30000, 5000 * (this.failures + 1)); // 5 s growing to 30 s
      this.state$.next(this.failures >= 3 ? 'offline' : 'reconnecting');
    });
    this.client = client;
    this.zone.runOutsideAngular(() => client.activate());
  }

  private shutdown(): void {
    const c = this.client;
    this.client = undefined;
    this.failures = 0;
    if (c) void c.deactivate();
    if (!this.paused) this.state$.next('idle');
  }

  private closeAll(): void {
    this.watchers.forEach(w => w.sub?.unsubscribe());
    this.watchers.clear();
    this.paused = false;
    this.shutdown();
    this.state$.next('idle');
  }

  /** Pause after 2 minutes with the tab hidden; on return, resume and signal one REST refresh. */
  private onVisibility(): void {
    if (document.hidden) {
      if (this.watchers.size && !this.hiddenTimer) {
        this.hiddenTimer = setTimeout(() => {
          this.hiddenTimer = undefined;
          this.paused = true;
          this.watchers.forEach(w => { w.sub?.unsubscribe(); w.sub = undefined; });
          const c = this.client;
          this.client = undefined;
          if (c) void c.deactivate();
          this.state$.next('idle');
        }, 120000);
      }
      return;
    }
    if (this.hiddenTimer) { clearTimeout(this.hiddenTimer); this.hiddenTimer = undefined; }
    if (this.paused) {
      this.paused = false;
      if (this.watchers.size) { this.ensureClient(); this.resumed$.next(); }
    }
  }
}
