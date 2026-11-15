import { Component, Input } from '@angular/core';

export interface ModuleTab { label: string; link: string; exact?: boolean; }

/** Horizontal sub-navigation under the module header (same style as the workspace sidebar buttons). */
@Component({
  selector: 'tx-module-tabs',
  templateUrl: './tx-module-tabs.component.html',
  styleUrls: ['./tx-module-tabs.component.scss']
})
export class TxModuleTabsComponent {
  @Input() tabs: ModuleTab[] = [];
}
