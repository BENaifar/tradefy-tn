import { FormControl, FormGroup } from '@angular/forms';
import {
  CSV_HEADER, checkCsvFile, dateRangeValidator, decimalValidator, decimalsOf, exactTextValidator, integerValidator, parseDecimal
} from './tx-validators';

describe('tx-validators', () => {
  describe('parseDecimal', () => {
    it('accepts comma or dot and rejects junk', () => {
      expect(parseDecimal('12,5')).toBe(12.5);
      expect(parseDecimal(' 7.250 ')).toBe(7.25);
      expect(parseDecimal('abc')).toBeNull();
      expect(parseDecimal('')).toBeNull();
      expect(parseDecimal('1e5')).toBeNull();
    });
    it('counts typed decimals', () => {
      expect(decimalsOf('1.2345')).toBe(4);
      expect(decimalsOf('10')).toBe(0);
      expect(decimalsOf('3,14')).toBe(2);
    });
  });

  describe('money amount rules (deposit / withdraw)', () => {
    const v = decimalValidator({ min: 0.001, max: 1_000_000, maxDecimals: 3, required: true });
    it('blocks 0 and more than 3 decimals', () => {
      expect(v(new FormControl('0'))).not.toBeNull();
      expect(v(new FormControl('1.2345'))).not.toBeNull();
    });
    it('allows 0.001, 1 000 000 and comma decimals', () => {
      expect(v(new FormControl('0.001'))).toBeNull();
      expect(v(new FormControl('1000000'))).toBeNull();
      expect(v(new FormControl('12,5'))).toBeNull();
    });
    it('blocks more than the cash operation limit and empty values', () => {
      expect(v(new FormControl('1000000.001'))).not.toBeNull();
      expect(v(new FormControl(''))).not.toBeNull();
    });
  });

  describe('integerValidator (trade quantity)', () => {
    const v = integerValidator(1);
    it('requires whole numbers of at least 1', () => {
      expect(v(new FormControl('100'))).toBeNull();
      expect(v(new FormControl('0'))).not.toBeNull();
      expect(v(new FormControl('2.5'))).not.toBeNull();
      expect(v(new FormControl('-3'))).not.toBeNull();
      expect(v(new FormControl(''))).not.toBeNull();
    });
  });

  it('exactTextValidator needs the exact word', () => {
    const v = exactTextValidator('RESET');
    expect(v(new FormControl('RESET'))).toBeNull();
    expect(v(new FormControl('reset'))).not.toBeNull();
  });

  describe('dateRangeValidator', () => {
    const today = () => '2026-11-15';
    const group = (from: string, to: string) => new FormGroup({ from: new FormControl(from), to: new FormControl(to) });
    const v = dateRangeValidator('from', 'to', today);
    it('accepts from ≤ to in the past', () => expect(v(group('2026-11-01', '2026-11-10'))).toBeNull());
    it('rejects from > to', () => expect(v(group('2026-11-10', '2026-11-01'))).not.toBeNull());
    it('rejects future dates', () => expect(v(group('2026-11-01', '2026-11-30'))).not.toBeNull());
  });

  describe('checkCsvFile', () => {
    const file = (name: string, content: string) => new File([content], name, { type: 'text/csv' });
    it('rejects non-csv and empty files', async () => {
      expect((await checkCsvFile(file('a.txt', 'x'))).ok).toBeFalse();
      expect((await checkCsvFile(file('a.csv', ''))).ok).toBeFalse();
    });
    it('checks the header and returns a preview', async () => {
      const bad = await checkCsvFile(file('a.csv', 'date,ticker\n2026-01-01,TJARI'));
      expect(bad.ok).toBeFalse();
      const rows = [CSV_HEADER, ...Array.from({ length: 8 }, (_, i) => `2026-01-0${i + 1},TJARI,1,2,1,2,100`)].join('\n');
      const ok = await checkCsvFile(file('a.csv', rows));
      expect(ok.ok).toBeTrue();
      expect(ok.preview.length).toBe(6); // header + 5 rows
    });
  });
});
