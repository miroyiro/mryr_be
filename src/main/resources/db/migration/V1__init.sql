-- =====================================================================
-- Flyway V1__init.sql — MIROYIRO main DB (AWS RDS, PostgreSQL 16)
-- 기준: ERD v1.2 (2026-11-06) 의 miroyiro_main 부분 = docs/05_erd_final.sql 과 동일
--
-- ⚠️ 한 번이라도 어느 DB에 적용된 뒤에는 이 파일을 절대 수정하지 않는다.
--    (Flyway가 체크섬으로 검사 → 수정 시 validate 실패). 변경은 V2__*.sql 로.
-- 전제: spring.flyway.schemas=miroyiro_main (스키마·이력 테이블 위치)
--       DB timezone = Asia/Seoul  (ALTER DATABASE miroyiro SET timezone='Asia/Seoul';)
--       pg_trgm 설치 권한 (RDS master 계정이면 OK)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE SCHEMA IF NOT EXISTS miroyiro_main;

CREATE OR REPLACE FUNCTION public.set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

SET search_path TO miroyiro_main, public;
-- ---------------------------------------------------------------------
-- 1. USER / AUTH
-- ---------------------------------------------------------------------
CREATE TABLE users (                                        -- `user`는 예약어 → users
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nickname       VARCHAR(30)  NOT NULL,                     -- 화면 표시 이름. 가입 시 입력
  email          VARCHAR(100) NULL,                         -- 카카오는 이메일 미제공 가능
  password_hash  VARCHAR(255) NULL,                         -- bcrypt. 소셜 로그인은 NULL
  oauth_provider VARCHAR(10)  NOT NULL DEFAULT 'local'
                 CHECK (oauth_provider IN ('local','kakao','naver','google')),  -- apple은 iOS 출시 때
  oauth_id       VARCHAR(100) NULL,
  profile_image  VARCHAR(500) NULL,
  role           VARCHAR(10)  NOT NULL DEFAULT 'user' CHECK (role IN ('user','admin')),
  onboarded_at   TIMESTAMPTZ  NULL,                         -- NULL이면 온보딩 화면으로
  last_login_at  TIMESTAMPTZ  NULL,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at     TIMESTAMPTZ  NULL,                         -- 회원 탈퇴 (soft delete)
  CONSTRAINT uq_user_nickname UNIQUE (nickname),
  CONSTRAINT uq_user_email    UNIQUE (email),
  CONSTRAINT uq_user_oauth    UNIQUE (oauth_provider, oauth_id)
);

CREATE TABLE user_refresh_token (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  CHAR(64)     NOT NULL,                        -- SHA-256 해시 (원문 저장 금지)
  user_agent  VARCHAR(255) NULL,
  expires_at  TIMESTAMPTZ  NOT NULL,
  revoked_at  TIMESTAMPTZ  NULL,                            -- 로그아웃 시각
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_user ON user_refresh_token (user_id);

-- ---------------------------------------------------------------------
-- 2. ARTIST (J-pop)
-- ---------------------------------------------------------------------
CREATE TABLE artist (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,                      -- 대표 표기명(한글) 예: 요아소비
  name_en       VARCHAR(100) NULL,
  name_ja       VARCHAR(100) NULL,
  category      VARCHAR(10)  NOT NULL DEFAULT 'jpop' CHECK (category IN ('jpop','anime')),
  profile_image VARCHAR(500) NULL,
  description   TEXT         NULL,
  debut_date    DATE         NULL,
  official_url  VARCHAR(500) NULL,                          -- 표시용 링크만 (수집 대상 아님)
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX trgm_artist_name    ON artist USING gin (name    gin_trgm_ops);
CREATE INDEX trgm_artist_name_en ON artist USING gin (name_en gin_trgm_ops);
CREATE INDEX trgm_artist_name_ja ON artist USING gin (name_ja gin_trgm_ops);

CREATE TABLE album (                                        -- 소스 미정 → 관리자 수기 입력 (07 #3)
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  artist_id    BIGINT       NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
  title        VARCHAR(200) NOT NULL,
  album_type   VARCHAR(10)  NOT NULL DEFAULT 'album' CHECK (album_type IN ('album','ep','single')),
  cover_image  VARCHAR(500) NULL,                           -- 사용 가능 여부 로톡 답변 대기
  release_date DATE         NULL,
  external_url VARCHAR(500) NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_album_artist_release ON album (artist_id, release_date);

CREATE TABLE song (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  artist_id         BIGINT       NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
  album_id          BIGINT       NULL     REFERENCES album(id)  ON DELETE SET NULL,  -- 디지털 싱글이면 NULL
  title             VARCHAR(200) NOT NULL,
  track_no          INT          NULL,
  release_date      DATE         NULL,
  is_representative BOOLEAN      NOT NULL DEFAULT FALSE,   -- 아티스트 허브 "대표곡"
  external_url      VARCHAR(500) NULL,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_song_artist ON song (artist_id, is_representative);
CREATE INDEX idx_song_album  ON song (album_id);

-- ---------------------------------------------------------------------
-- 3. ANIME  (catalog = AniList lazy fetch + TTL, NO bulk seed)
--    외부 API 호출은 전부 scrap이 함 → 결과만 내부 API로 upsert
-- ---------------------------------------------------------------------
CREATE TABLE anime (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  anilist_id         INT          NULL,                     -- AniList Media.id (리졸빙 전 NULL)
  anissia_no         INT          NULL,                     -- 애니시아 animeNo
  tmdb_id            INT          NULL,
  tmdb_season_number INT          NULL,                     -- TMDB 시리즈 안 몇 시즌인지
  wikidata_qid       VARCHAR(20)  NULL,                     -- 보조 (컬렉터 미구현)
  laftel_id          VARCHAR(30)  NULL,
  title_ko           VARCHAR(200) NULL,                     -- 애니시아 subject
  title_romaji       VARCHAR(200) NOT NULL,
  title_native       VARCHAR(200) NULL,
  title_en           VARCHAR(200) NULL,
  format             VARCHAR(10)  NULL CHECK (format IN ('tv','tv_short','movie','ova','ona','special','music')),
  status             VARCHAR(20)  NULL CHECK (status IN ('not_yet_released','releasing','finished','cancelled','hiatus')),
  season             VARCHAR(10)  NULL CHECK (season IN ('winter','spring','summer','fall')),
  season_year        SMALLINT     NULL,
  episodes           INT          NULL,
  genres             JSONB        NULL,                     -- ["Fantasy","Adventure"]
  studios            JSONB        NULL,                     -- ["MADHOUSE"] (isMain만)
  staff              JSONB        NULL,                     -- [{"name":"..","role":"Director"}]
  age_rating         VARCHAR(20)  NULL,
  poster_url         VARCHAR(500) NULL,                     -- AniList/TMDB 제공 URL만
  synopsis_ko        TEXT         NULL,                     -- TMDB ko-KR
  synopsis_en        TEXT         NULL,                     -- AniList (fallback)
  broadcast_weekday  SMALLINT     NULL CHECK (broadcast_weekday BETWEEN 0 AND 6),  -- 0=일
  broadcast_time     TIME         NULL,                     -- KST
  official_url       VARCHAR(500) NULL,                     -- 표시용 링크만 (애니시아 제공)
  official_x         VARCHAR(100) NULL,
  synced_at          TIMESTAMPTZ  NULL,                     -- TTL: releasing 1일 / finished 30일
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_anime_anilist UNIQUE (anilist_id),
  CONSTRAINT uq_anime_anissia UNIQUE (anissia_no)
);
CREATE INDEX idx_anime_season ON anime (season_year, season);
CREATE INDEX idx_anime_status ON anime (status);
CREATE INDEX trgm_anime_title_ko     ON anime USING gin (title_ko     gin_trgm_ops);
CREATE INDEX trgm_anime_title_romaji ON anime USING gin (title_romaji gin_trgm_ops);
CREATE INDEX trgm_anime_title_native ON anime USING gin (title_native gin_trgm_ops);
CREATE INDEX trgm_anime_title_en     ON anime USING gin (title_en     gin_trgm_ops);

CREATE TABLE anime_character (
  id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  anime_id             BIGINT       NOT NULL REFERENCES anime(id) ON DELETE CASCADE,
  anilist_character_id INT          NULL,
  name                 VARCHAR(100) NOT NULL,
  image_url            VARCHAR(500) NULL,                   -- AniList 제공 URL만
  role                 VARCHAR(12)  NOT NULL DEFAULT 'supporting' CHECK (role IN ('main','supporting','background')),
  voice_actor_name     VARCHAR(100) NULL,                   -- 일본어 성우
  sort_order           INT          NOT NULL DEFAULT 0,
  CONSTRAINT uq_character_anime UNIQUE (anime_id, anilist_character_id)
);

CREATE TABLE anime_episode (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  anime_id     BIGINT       NOT NULL REFERENCES anime(id) ON DELETE CASCADE,
  episode_no   INT          NOT NULL,                       -- 이 쿨 기준 (1화)
  overall_no   INT          NULL,                           -- 시리즈 전체 기준 (29화)
  title        VARCHAR(200) NULL,
  air_date     DATE         NULL,
  duration_min INT          NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_episode UNIQUE (anime_id, episode_no)
);

CREATE TABLE anime_streaming (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  anime_id    BIGINT       NOT NULL REFERENCES anime(id) ON DELETE CASCADE,
  platform    VARCHAR(20)  NOT NULL CHECK (platform IN ('netflix','tving','watcha','wavve','disney_plus','coupang_play','laftel','aniplus_tv','etc')),
  url         VARCHAR(500) NULL,                            -- TMDB 출처는 딥링크 없어 NULL 가능. 넷플릭스 = netflix.com/title/{id} 링크만
  source      VARCHAR(10)  NOT NULL CHECK (source IN ('manual','tmdb','anilist','wikidata','laftel','aniplus')),
  verified_at TIMESTAMPTZ  NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_streaming UNIQUE (anime_id, platform, source)
  -- 화면 우선순위(앱에서 적용): manual > laftel·aniplus(직접 확인, 딥링크 있음) > tmdb > anilist > wikidata
);

CREATE TABLE anime_review (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  anime_id   BIGINT      NOT NULL REFERENCES anime(id) ON DELETE CASCADE,
  user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  rating     SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
  content    TEXT        NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_review_user_anime UNIQUE (anime_id, user_id)   -- 작품당 1인 1개
);
CREATE INDEX idx_review_anime_created ON anime_review (anime_id, created_at);

-- ---------------------------------------------------------------------
-- 4. EVENT (jpop 공연 + anime 방영/팝업/카페/페어 — 한 테이블)
-- ---------------------------------------------------------------------
CREATE TABLE event (
  id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  category             VARCHAR(10)  NOT NULL CHECK (category IN ('jpop','anime')),
  artist_id            BIGINT       NULL REFERENCES artist(id) ON DELETE SET NULL,
  anime_id             BIGINT       NULL REFERENCES anime(id)  ON DELETE SET NULL,
  type                 VARCHAR(20)  NOT NULL CHECK (type IN ('concert','broadcast','streaming','popup','collabo_cafe','fair','exhibition','release','etc')),
  title                VARCHAR(200) NOT NULL,
  venue                VARCHAR(200) NULL,
  address              VARCHAR(255) NULL,
  city                 VARCHAR(50)  NULL,
  country              CHAR(2)      NOT NULL DEFAULT 'KR',
  lat                  NUMERIC(9,6) NULL,                    -- 덕질지도 마커
  lng                  NUMERIC(9,6) NULL,
  start_at             TIMESTAMPTZ  NOT NULL,
  end_at               TIMESTAMPTZ  NULL,
  is_all_day           BOOLEAN      NOT NULL DEFAULT FALSE,
  status               VARCHAR(10)  NOT NULL DEFAULT 'upcoming' CHECK (status IN ('upcoming','ongoing','ended','canceled')),
  is_international     BOOLEAN      NOT NULL DEFAULT FALSE,  -- 내한 (KOPIS visit=Y)
  reservation_required BOOLEAN      NULL,
  poster_url           VARCHAR(500) NULL,
  description          TEXT         NULL,
  price_info           VARCHAR(255) NULL,
  ticket_url           VARCHAR(500) NULL,
  source               VARCHAR(20)  NOT NULL,                -- ck_event_source (v1.2: official 제거)
  source_id            VARCHAR(100) NULL,                    -- KOPIS mt20id, 애니시아 animeNo+날짜 등
  source_url           VARCHAR(500) NULL,                    -- 1차 출처 (manual이면 필수 → ck_event_manual_url)
  processed_item_id    BIGINT       NULL,                    -- scrap 참조값 (FK 없음). 승인 재시도 멱등키
  created_by           BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
  view_count           INT          NOT NULL DEFAULT 0 CHECK (view_count >= 0),
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ck_event_source          CHECK (source IN ('kopis','anissia','animate','aniplus','x','umj','manual')),
  CONSTRAINT ck_event_manual_url      CHECK (source <> 'manual' OR source_url IS NOT NULL),
  CONSTRAINT uq_event_source          UNIQUE (source, source_id),
  CONSTRAINT uq_event_processed_item  UNIQUE (processed_item_id),
  CONSTRAINT ck_event_dates           CHECK (end_at IS NULL OR end_at >= start_at)
);
CREATE INDEX idx_event_cat_start    ON event (category, start_at);
CREATE INDEX idx_event_artist_start ON event (artist_id, start_at);
CREATE INDEX idx_event_anime_start  ON event (anime_id, start_at);
CREATE INDEX idx_event_type_status  ON event (type, status);
CREATE INDEX trgm_event_title ON event USING gin (title gin_trgm_ops);
CREATE INDEX trgm_event_venue ON event USING gin (venue gin_trgm_ops);

-- ---------------------------------------------------------------------
-- 5. NEWS (홈 "놓친 소식")
-- ---------------------------------------------------------------------
CREATE TABLE news_feed (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  category          VARCHAR(10)  NOT NULL CHECK (category IN ('jpop','anime')),
  artist_id         BIGINT       NULL REFERENCES artist(id) ON DELETE SET NULL,
  anime_id          BIGINT       NULL REFERENCES anime(id)  ON DELETE SET NULL,
  source            VARCHAR(20)  NOT NULL,                  -- ck_news_source (v1.2: official·kopis 제거)
  source_url        VARCHAR(500) NOT NULL,                  -- 아웃링크 (본문 저장 X)
  title             VARCHAR(300) NOT NULL,
  summary           VARCHAR(500) NULL,
  thumbnail_url     VARCHAR(500) NULL,                      -- AniList/TMDB 이미지만
  published_at      TIMESTAMPTZ  NULL,
  processed_item_id BIGINT       NULL,                      -- scrap 참조값. 승인 재시도 멱등키
  like_count        INT          NOT NULL DEFAULT 0 CHECK (like_count >= 0),
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ck_news_source          CHECK (source IN ('aniplus','x','umj','manual')),
  CONSTRAINT uq_news_url             UNIQUE (source_url),
  CONSTRAINT uq_news_processed_item  UNIQUE (processed_item_id)
);
CREATE INDEX idx_news_artist_pub ON news_feed (artist_id, published_at);
CREATE INDEX idx_news_anime_pub  ON news_feed (anime_id, published_at);

CREATE TABLE news_like (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id      BIGINT      NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
  news_feed_id BIGINT      NOT NULL REFERENCES news_feed(id) ON DELETE CASCADE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_news_like UNIQUE (user_id, news_feed_id)
);
CREATE INDEX idx_like_news ON news_like (news_feed_id);

-- ---------------------------------------------------------------------
-- 6. FOLLOW (자유입력 → 리졸빙 → 매칭)
-- ---------------------------------------------------------------------
CREATE TABLE user_artist_follow (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id      BIGINT       NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  artist_id    BIGINT       NULL     REFERENCES artist(id) ON DELETE CASCADE,  -- 매칭 전 NULL
  raw_input    VARCHAR(100) NULL,
  match_status VARCHAR(10)  NOT NULL DEFAULT 'pending' CHECK (match_status IN ('pending','matched','unmatched')),
  match_score  NUMERIC(4,3) NULL,                           -- RapidFuzz 0~1
  resolved_at  TIMESTAMPTZ  NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_artist_follow UNIQUE (user_id, artist_id)  -- NULL은 중복 허용
);
CREATE INDEX idx_artist_follow_status ON user_artist_follow (match_status);
CREATE INDEX idx_artist_follow_artist ON user_artist_follow (artist_id);

CREATE TABLE user_anime_follow (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  anime_id     BIGINT       NULL     REFERENCES anime(id) ON DELETE CASCADE,
  raw_input    VARCHAR(100) NULL,
  match_status VARCHAR(10)  NOT NULL DEFAULT 'pending' CHECK (match_status IN ('pending','matched','unmatched')),
  match_score  NUMERIC(4,3) NULL,
  resolved_at  TIMESTAMPTZ  NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_anime_follow UNIQUE (user_id, anime_id)
);
CREATE INDEX idx_anime_follow_status ON user_anime_follow (match_status);
CREATE INDEX idx_anime_follow_anime  ON user_anime_follow (anime_id);

-- ---------------------------------------------------------------------
-- 7. CALENDAR + PUSH (개인 일정 X)
-- ---------------------------------------------------------------------
CREATE TABLE user_calendar (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_id   BIGINT       NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  status     VARCHAR(10)  NOT NULL DEFAULT 'planned' CHECK (status IN ('planned','done','canceled')),
  memo       VARCHAR(500) NULL,
  is_alert   BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_calendar_user_event UNIQUE (user_id, event_id)
);
CREATE INDEX idx_calendar_event ON user_calendar (event_id);

CREATE TABLE notification_log (
  id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_calendar_id BIGINT      NOT NULL REFERENCES user_calendar(id) ON DELETE CASCADE,
  stage            VARCHAR(5)  NOT NULL CHECK (stage IN ('d14','d5')),
  result           VARCHAR(10) NOT NULL DEFAULT 'sent' CHECK (result IN ('sent','failed')),
  sent_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_notification_once UNIQUE (user_calendar_id, stage)
);

CREATE TABLE user_push_subscription (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  endpoint   VARCHAR(500) NOT NULL,
  p256dh     VARCHAR(255) NOT NULL,
  auth       VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_push_endpoint UNIQUE (endpoint)
);
CREATE INDEX idx_push_user ON user_push_subscription (user_id);

-- ---------------------------------------------------------------------
-- 8. STREAK / ACTIVITY
-- ---------------------------------------------------------------------
CREATE TABLE user_activity_log (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       VARCHAR(20) NOT NULL CHECK (type IN ('login','like','news_view','follow','calendar_register')),
  ref_id     BIGINT      NULL,                              -- 다형 참조 (FK 없음)
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_activity_user_time ON user_activity_log (user_id, created_at);

CREATE TABLE streak (
  id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  date    DATE     NOT NULL,
  score   SMALLINT NOT NULL DEFAULT 0 CHECK (score BETWEEN 0 AND 5),  -- 잔디 진하기
  CONSTRAINT uq_streak_user_date UNIQUE (user_id, date)
);

-- ---------------------------------------------------------------------
-- 9. SEARCH
-- ---------------------------------------------------------------------
CREATE TABLE user_search_history (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,  -- 비로그인도 집계
  keyword     VARCHAR(100) NOT NULL,
  searched_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  hidden_at   TIMESTAMPTZ  NULL                              -- "최근 검색어" 삭제 = 숨김만
);
CREATE INDEX idx_search_user_time ON user_search_history (user_id, searched_at);
CREATE INDEX idx_search_time      ON user_search_history (searched_at);

CREATE TABLE popular_keyword (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  snapshot_at TIMESTAMPTZ  NOT NULL,                         -- 1시간마다
  rank_no     SMALLINT     NOT NULL,
  keyword     VARCHAR(100) NOT NULL,
  trend       VARCHAR(5)   NOT NULL DEFAULT 'same' CHECK (trend IN ('up','down','same','new')),
  CONSTRAINT uq_popular_snapshot_rank UNIQUE (snapshot_at, rank_no)
);

-- ---------------------------------------------------------------------
-- updated_at 트리거 (miroyiro_main 안에서 updated_at 있는 테이블 전부)
-- ---------------------------------------------------------------------
DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT table_name FROM information_schema.columns
    WHERE column_name = 'updated_at' AND table_schema = 'miroyiro_main'
  LOOP
    EXECUTE format(
      'CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON miroyiro_main.%I
         FOR EACH ROW EXECUTE FUNCTION public.set_updated_at()',
      r.table_name, r.table_name);
  END LOOP;
END $$;

RESET search_path;
