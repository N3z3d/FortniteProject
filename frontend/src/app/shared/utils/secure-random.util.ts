const UINT32_RANGE = 0x1_0000_0000;
const ID_CHARSET = 'abcdefghijklmnopqrstuvwxyz0123456789';

function randomUint32(): number {
  const buffer = new Uint32Array(1);
  globalThis.crypto.getRandomValues(buffer);
  return buffer[0];
}

export function secureRandomFloat(): number {
  return randomUint32() / UINT32_RANGE;
}

export function secureRandomInt(maxExclusive: number): number {
  if (!Number.isInteger(maxExclusive) || maxExclusive <= 0) {
    throw new Error('maxExclusive must be a positive integer');
  }

  const rejectionLimit = UINT32_RANGE - (UINT32_RANGE % maxExclusive);
  let value = randomUint32();
  while (value >= rejectionLimit) {
    value = randomUint32();
  }

  return value % maxExclusive;
}

export function secureRandomIntInRange(minInclusive: number, maxInclusive: number): number {
  if (!Number.isInteger(minInclusive) || !Number.isInteger(maxInclusive)) {
    throw new Error('Range bounds must be integers');
  }
  if (maxInclusive < minInclusive) {
    throw new Error('maxInclusive must be greater than or equal to minInclusive');
  }

  const rangeSize = maxInclusive - minInclusive + 1;
  return minInclusive + secureRandomInt(rangeSize);
}

export function secureRandomPick<T>(values: readonly T[]): T {
  if (values.length === 0) {
    throw new Error('values must not be empty');
  }

  return values[secureRandomInt(values.length)];
}

export function secureRandomId(length: number = 9): string {
  if (!Number.isInteger(length) || length <= 0) {
    throw new Error('length must be a positive integer');
  }

  let id = '';
  for (let i = 0; i < length; i++) {
    id += ID_CHARSET[secureRandomInt(ID_CHARSET.length)];
  }
  return id;
}
