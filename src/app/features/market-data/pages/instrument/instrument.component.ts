import { Component } from '@angular/core';
import { MARKET_TABS } from '../../../../shared/ui/util/module-tabs';

/** Placeholder page, filled in by a later phase. */
@Component({
  selector: 'app-instrument',
  templateUrl: './instrument.component.html',
  styleUrls: ['./instrument.component.scss']
})
export class InstrumentComponent {
  readonly tabs = MARKET_TABS;
}
