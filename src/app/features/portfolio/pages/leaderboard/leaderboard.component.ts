import { Component, OnDestroy, OnInit } from '@angular/core';
import { SegmentOption } from '../../../../shared/ui/components/tx-segmented/tx-segmented.component';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';
import { lsGet, lsSet } from '../../../../shared/ui/util/safe-storage';
import { Section } from '../../../../shared/ui/util/section';
import { ToastService } from '../../../../shared/ui/util/toast.service';
import { LeaderboardEntry } from '../../models/portfolio.model';
import { PerformanceService } from '../../services/performance.service';

/** Leaderboard (route 34): podium for the top 3, then the rest; the user's own row is highlighted and pinned. */
@Component({
  selector: 'app-leaderboard',
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.scss']
})
export class LeaderboardComponent implements OnInit, OnDestroy {
  readonly tabs = PORTFOLIO_TABS;
  readonly limitOptions: SegmentOption[] = [{ value: '10', label: 'Top 10' }, { value: '25', label: 'Top 25' }, { value: '50', label: 'Top 50' }];
  limit = 10;
  board: Section<LeaderboardEntry[]>;
  /** The user's row when it is not inside the requested top N (looked up once with the maximum limit). */
  pinned: LeaderboardEntry | null = null;

  constructor(private api: PerformanceService, toast: ToastService) {
    this.board = new Section<LeaderboardEntry[]>(toast, list => this.findMe(list));
  }

  ngOnInit(): void {
    const l = lsGet<number>('leaderboard_limit', 10);
    this.limit = [10, 25, 50].includes(l) ? l : 10;
    this.load();
  }
  ngOnDestroy(): void { this.board.destroy(); }

  load(): void { this.board.load(() => this.api.leaderboard(this.limit), true); }
  setLimit(v: string): void { this.limit = Number(v); lsSet('leaderboard_limit', this.limit); this.pinned = null; this.load(); }

  private findMe(list: LeaderboardEntry[]): void {
    this.pinned = null;
    if (list.some(e => e.isMe)) return;
    this.api.leaderboard(100).subscribe({ next: all => { this.pinned = all.find(e => e.isMe) ?? null; }, error: () => undefined });
  }

  get podium(): LeaderboardEntry[] { return (this.board.data ?? []).slice(0, 3); }
  get rest(): LeaderboardEntry[] { return (this.board.data ?? []).slice(3); }
  trackRank(_: number, e: LeaderboardEntry): number { return e.rank; }
}
