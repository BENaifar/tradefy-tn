import { Component } from '@angular/core';
import { MARKET_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-markets',
  templateUrl: './markets.component.html',
  styleUrls: ['./markets.component.scss']
})
export class MarketsComponent {
  readonly tabs = MARKET_TABS;
}
