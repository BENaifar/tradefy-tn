import { Component } from '@angular/core';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-performance',
  templateUrl: './performance.component.html',
  styleUrls: ['./performance.component.scss']
})
export class PerformanceComponent {
  readonly tabs = PORTFOLIO_TABS;
}
