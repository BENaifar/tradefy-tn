import { Component } from '@angular/core';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-positions',
  templateUrl: './positions.component.html',
  styleUrls: ['./positions.component.scss']
})
export class PositionsComponent {
  readonly tabs = PORTFOLIO_TABS;
}
