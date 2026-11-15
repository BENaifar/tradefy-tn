import { Component } from '@angular/core';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-activity',
  templateUrl: './activity.component.html',
  styleUrls: ['./activity.component.scss']
})
export class ActivityComponent {
  readonly tabs = PORTFOLIO_TABS;
}
