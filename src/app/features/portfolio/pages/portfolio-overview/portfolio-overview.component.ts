import { Component } from '@angular/core';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-portfolio-overview',
  templateUrl: './portfolio-overview.component.html',
  styleUrls: ['./portfolio-overview.component.scss']
})
export class PortfolioOverviewComponent {
  readonly tabs = PORTFOLIO_TABS;
}
