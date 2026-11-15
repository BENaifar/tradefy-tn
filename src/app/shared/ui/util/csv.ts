/** Downloads rows as a CSV file (what is on screen). Values are quoted when needed. */
export function downloadCsv(filename: string, header: string[], rows: (string | number | null | undefined)[][]): void {
  const esc = (v: string | number | null | undefined): string => {
    const s = v === null || v === undefined ? '' : String(v);
    // neutralise spreadsheet formula injection
    const safe = /^[=+\-@]/.test(s) && isNaN(Number(s)) ? "'" + s : s;
    return /[",\n;]/.test(safe) ? '"' + safe.replace(/"/g, '""') + '"' : safe;
  };
  const text = [header, ...rows].map(r => r.map(esc).join(',')).join('\n');
  const url = URL.createObjectURL(new Blob([text], { type: 'text/csv;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
