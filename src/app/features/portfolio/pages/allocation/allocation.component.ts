import { Component } from '@angular/core';
import { PORTFOLIO_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-allocation',
  templateUrl: './allocation.component.html',
  styleUrls: ['./allocation.component.scss']
})
export class AllocationComponent {
  readonly tabs = PORTFOLIO_TABS;
}
