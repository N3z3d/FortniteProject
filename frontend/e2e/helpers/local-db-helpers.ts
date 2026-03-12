import { execFileSync } from 'node:child_process';

const LOCAL_POSTGRES_CONTAINER =
  process.env['LOCAL_POSTGRES_CONTAINER'] ?? 'fortnite-postgres-local';
const LOCAL_POSTGRES_USER = process.env['LOCAL_POSTGRES_USER'] ?? 'fortnite_user';
const LOCAL_POSTGRES_DB = process.env['LOCAL_POSTGRES_DB'] ?? 'fortnite_pronos';

export function softDeleteLocalGamesByPrefix(prefix: string): void {
  const likePattern = `${escapeSqlLiteral(prefix)}%`;
  runSoftDelete(`name LIKE '${likePattern}' AND deleted_at IS NULL`);
}

export function softDeleteLocalGameById(gameId: string): void {
  runSoftDelete(`id = '${escapeSqlLiteral(gameId)}' AND deleted_at IS NULL`);
}

function runSoftDelete(whereClause: string): void {
  const sql = `UPDATE games SET deleted_at = NOW() WHERE ${whereClause};`;

  execFileSync(
    'docker',
    [
      'exec',
      LOCAL_POSTGRES_CONTAINER,
      'psql',
      '-U',
      LOCAL_POSTGRES_USER,
      '-d',
      LOCAL_POSTGRES_DB,
      '-c',
      sql,
    ],
    { stdio: 'pipe' }
  );
}

function escapeSqlLiteral(value: string): string {
  return value.replace(/'/g, "''");
}
