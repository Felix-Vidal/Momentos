import { inject } from '@angular/core';
import { GroupShellPage } from './group-shell.page';

/**
 * As abas rodam dentro do `GroupShellPage`, então podem injetá-lo para reaproveitar
 * o grupo já carregado em vez de buscá-lo outra vez.
 */
export function injectGroup() {
  const shell = inject(GroupShellPage);
  return { group: shell.group, groupId: shell.groupId };
}
