import { Component } from '@angular/core';
import { MARKET_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-watchlist',
  templateUrl: './watchlist.component.html',
  styleUrls: ['./watchlist.component.scss']
})
export class WatchlistComponent {
  readonly tabs = MARKET_TABS;
}
