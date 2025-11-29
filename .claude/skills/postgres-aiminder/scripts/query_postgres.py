#!/usr/bin/env python3
"""
AiMinder PostgreSQL 쿼리 실행 스크립트

사용법:
    python query_postgres.py "SELECT * FROM users LIMIT 5"
    python query_postgres.py "SELECT * FROM goals" --json
    python query_postgres.py "SHOW TABLES" --count
    
연결 정보는 환경변수 또는 기본값(로컬 개발용)을 사용합니다.
"""

import os
import sys
import json
import argparse
from datetime import datetime, date
from decimal import Decimal

try:
    import psycopg2
    from psycopg2.extras import RealDictCursor
except ImportError:
    print("Error: psycopg2 모듈이 설치되지 않았습니다.")
    print("설치 명령어: pip install psycopg2-binary")
    sys.exit(1)


class DateTimeEncoder(json.JSONEncoder):
    """JSON 직렬화를 위한 날짜/시간 인코더"""
    def default(self, obj):
        if isinstance(obj, (datetime, date)):
            return obj.isoformat()
        if isinstance(obj, Decimal):
            return float(obj)
        return super().default(obj)


def get_connection():
    """환경변수 또는 기본값을 사용해서 데이터베이스 연결"""
    
    # AiMinder 프로젝트 기본값 (로컬 개발용)
    defaults = {
        'host': 'localhost',
        'port': '5432',
        'database': 'aiminderdb',
        'user': 'aiminder',
        'password': 'aiminder'
    }
    
    # 환경변수에서 값 읽기 (여러 형식 지원)
    config = {
        'host': os.environ.get('DATABASE_HOST', os.environ.get('POSTGRES_HOST', defaults['host'])),
        'port': os.environ.get('DATABASE_PORT', os.environ.get('POSTGRES_PORT', defaults['port'])),
        'database': os.environ.get('DATABASE_NAME', os.environ.get('POSTGRES_DB', defaults['database'])),
        'user': os.environ.get('DATABASE_USERNAME', os.environ.get('POSTGRES_USER', defaults['user'])),
        'password': os.environ.get('DATABASE_PASSWORD', os.environ.get('POSTGRES_PASSWORD', defaults['password']))
    }
    
    try:
        print(f"🔌 연결 중: {config['user']}@{config['host']}:{config['port']}/{config['database']}")
        return psycopg2.connect(**config)
    except psycopg2.Error as e:
        print(f"❌ 데이터베이스 연결 실패: {e}", file=sys.stderr)
        print("\n💡 문제 해결 방법:", file=sys.stderr)
        print("1. Docker PostgreSQL 컨테이너가 실행 중인지 확인: docker ps | grep postgres", file=sys.stderr)
        print("2. 환경변수 설정 확인 (DATABASE_HOST, DATABASE_PORT 등)", file=sys.stderr)
        print("3. 네트워크 연결 확인: telnet localhost 5432", file=sys.stderr)
        sys.exit(1)


def is_safe_query(query):
    """기본적인 쿼리 안전성 검사 (SELECT, SHOW, DESCRIBE 등만 허용)"""
    query_upper = query.strip().upper()
    safe_operations = ['SELECT', 'SHOW', 'DESCRIBE', 'EXPLAIN', 'WITH']
    
    return any(query_upper.startswith(op) for op in safe_operations)


def execute_query(query, output_format='table', limit_rows=None):
    """SQL 쿼리 실행 및 결과 반환"""
    
    if not is_safe_query(query):
        print("⚠️  경고: 안전성을 위해 SELECT, SHOW, DESCRIBE 쿼리만 허용됩니다.", file=sys.stderr)
        print(f"실행하려는 쿼리: {query[:50]}...", file=sys.stderr)
        response = input("계속 실행하시겠습니까? (y/N): ")
        if response.lower() != 'y':
            sys.exit(1)
    
    conn = None
    cur = None
    
    try:
        conn = get_connection()
        cur = conn.cursor(cursor_factory=RealDictCursor)
        
        # 실행 시간 측정
        start_time = datetime.now()
        cur.execute(query)
        execution_time = datetime.now() - start_time
        
        if cur.description:  # SELECT 쿼리인 경우
            rows = cur.fetchall()
            
            # 행 수 제한
            if limit_rows and len(rows) > limit_rows:
                rows = rows[:limit_rows]
                print(f"⚠️  결과가 {limit_rows}개 행으로 제한되었습니다.", file=sys.stderr)
            
            print(f"⏱️  실행 시간: {execution_time.total_seconds():.3f}초")
            
            if output_format == 'json':
                print(json.dumps(rows, cls=DateTimeEncoder, indent=2, ensure_ascii=False))
            elif output_format == 'count':
                print(f"총 {len(rows)}개 행")
                if rows:
                    headers = list(rows[0].keys())
                    print(f"컬럼: {', '.join(headers)}")
            else:  # table format
                if rows:
                    headers = list(rows[0].keys())
                    
                    # 컬럼 너비 계산
                    col_widths = {}
                    for header in headers:
                        col_widths[header] = max(
                            len(str(header)),
                            max(len(str(row[header] or '')) for row in rows) if rows else 0
                        )
                        # 최대 너비 제한
                        col_widths[header] = min(col_widths[header], 50)
                    
                    # 헤더 출력
                    header_line = " | ".join(str(header).ljust(col_widths[header]) for header in headers)
                    print(header_line)
                    print("-" * len(header_line))
                    
                    # 데이터 출력
                    for row in rows:
                        row_line = " | ".join(
                            str(row[header] or '')[:col_widths[header]].ljust(col_widths[header]) 
                            for header in headers
                        )
                        print(row_line)
                else:
                    print("🔍 조회 결과가 없습니다.")
            
            print(f"\n📊 총 {len(rows)}개 행 반환")
            
        else:  # INSERT, UPDATE, DELETE 등
            conn.commit()
            print(f"✅ 쿼리 실행 완료. 영향받은 행: {cur.rowcount}")
            print(f"⏱️  실행 시간: {execution_time.total_seconds():.3f}초")
        
    except psycopg2.Error as e:
        print(f"❌ 쿼리 실행 오류: {e}", file=sys.stderr)
        print(f"쿼리: {query}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"❌ 예상치 못한 오류: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()


def show_quick_commands():
    """자주 사용하는 명령어 보기"""
    commands = [
        ("테이블 목록", "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"),
        ("사용자 목록", "SELECT user_id, provider, provider_id, created_at FROM users LIMIT 10"),
        ("활성 목표", "SELECT title, description, target_date FROM goals WHERE status = 'ACTIVE' AND deleted_at IS NULL LIMIT 10"),
        ("최근 대화", "SELECT conversation_id, created_at FROM conversations ORDER BY created_at DESC LIMIT 10"),
        ("테이블 크기", "SELECT schemaname,tablename,attname,n_distinct,correlation FROM pg_stats WHERE schemaname = 'public'"),
        ("스키마 정보", "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'users'")
    ]
    
    print("🚀 자주 사용하는 쿼리 명령어:")
    print()
    for i, (desc, cmd) in enumerate(commands, 1):
        print(f"{i}. {desc}")
        print(f"   python {sys.argv[0]} \"{cmd}\"")
        print()


def main():
    parser = argparse.ArgumentParser(
        description="AiMinder PostgreSQL 쿼리 실행 도구",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
예시:
  python query_postgres.py "SELECT * FROM users LIMIT 5"
  python query_postgres.py "SELECT * FROM goals" --json
  python query_postgres.py "SELECT COUNT(*) FROM users" --count
  python query_postgres.py --help-queries
        """
    )
    
    parser.add_argument('query', nargs='?', help='실행할 SQL 쿼리')
    parser.add_argument('--json', action='store_true', help='JSON 형식으로 출력')
    parser.add_argument('--count', action='store_true', help='행 개수만 출력')
    parser.add_argument('--limit', type=int, help='출력할 최대 행 수 제한')
    parser.add_argument('--help-queries', action='store_true', help='자주 사용하는 쿼리 명령어 보기')
    
    args = parser.parse_args()
    
    if args.help_queries:
        show_quick_commands()
        return
    
    if not args.query:
        print("❌ SQL 쿼리를 입력해주세요.")
        parser.print_help()
        sys.exit(1)
    
    # 출력 형식 결정
    output_format = 'json' if args.json else 'count' if args.count else 'table'
    
    execute_query(args.query, output_format, args.limit)


if __name__ == '__main__':
    main()