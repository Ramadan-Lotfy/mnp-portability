import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { OperatorService } from './core/operator.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
})
export class App {
  protected readonly operator = inject(OperatorService);

  protected onOperatorChange(event: Event): void {
    this.operator.select((event.target as HTMLSelectElement).value);
  }
}
