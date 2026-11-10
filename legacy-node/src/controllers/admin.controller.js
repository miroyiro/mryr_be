import {
  updateAnimeStreaming,
  registerArtist,
  editArtist,
  registerArtistAlbum,
  registerArtistSong,
  registerAdminEvent,
  removeAdminEvent,
  editAdminEvent,
  getAdminFollowRequests,
  resolveAdminFollow,
} from "../services/admin.service.js";

const ALLOWED_PLATFORMS = new Set([
  "netflix",
  "tving",
  "watcha",
  "wavve",
  "disney_plus",
  "coupang_play",
  "laftel",
  "aniplus_tv",
  "etc",
]);

export async function updateAnimeStreamingController(req, res) {
  try {
    const animeId = Number(req.params.id);
    const { platforms } = req.body;

    if (!Number.isInteger(animeId) || animeId <= 0) {
      return res.status(400).json({
        error: {
          code: "INVALID_ANIME_ID",
          message: "유효하지 않은 애니메이션 ID입니다.",
        },
      });
    }

    if (!Array.isArray(platforms)) {
      return res.status(400).json({
        error: {
          code: "INVALID_PLATFORMS",
          message: "platforms는 배열이어야 합니다.",
        },
      });
    }

    const seenPlatforms = new Set();

    for (const item of platforms) {
      if (
        !item ||
        typeof item !== "object" ||
        Array.isArray(item) ||
        !ALLOWED_PLATFORMS.has(item.platform) ||
        seenPlatforms.has(item.platform)
      ) {
        return res.status(400).json({
          error: {
            code: "INVALID_PLATFORM",
            message: "플랫폼 값이 잘못되었거나 중복되었습니다.",
          },
        });
      }

      seenPlatforms.add(item.platform);

      if (item.url !== null) {
        if (typeof item.url !== "string" || item.url.trim() === "") {
          return res.status(400).json({
            error: {
              code: "INVALID_STREAMING_URL",
              message: "url은 유효한 URL 문자열 또는 null이어야 합니다.",
            },
          });
        }

        try {
          const parsedUrl = new URL(item.url);

          if (!["http:", "https:"].includes(parsedUrl.protocol)) {
            throw new Error("Unsupported URL protocol");
          }
        } catch {
          return res.status(400).json({
            error: {
              code: "INVALID_STREAMING_URL",
              message: "url은 올바른 HTTP 또는 HTTPS URL이어야 합니다.",
            },
          });
        }
      }
    }

    const result = await updateAnimeStreaming({ animeId, platforms });

    if (!result) {
      return res.status(404).json({
        error: {
          code: "ANIME_NOT_FOUND",
          message: "애니메이션을 찾을 수 없습니다.",
        },
      });
    }

    return res.status(200).json(result);
  } catch (error) {
    console.error("스트리밍 링크 수동 보정 오류:", error);

    return res.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "서버 내부 오류가 발생했습니다.",
      },
    });
  }
}

const ALLOWED_ARTIST_CATEGORIES = new Set(["jpop", "anime"]);

export async function createArtistController(req, res) {
  try {
    const {
      name,
      name_en,
      name_ja,
      category,
      profile_image,
      description,
      debut_date,
      official_url,
    } = req.body;

    // 필수값 검증
    if (typeof name !== "string" || name.trim() === "") {
      return res.status(400).json({
        error: {
          code: "INVALID_NAME",
          message: "아티스트 이름은 필수입니다.",
        },
      });
    }

    if (!ALLOWED_ARTIST_CATEGORIES.has(category)) {
      return res.status(400).json({
        error: {
          code: "INVALID_CATEGORY",
          message: "category는 jpop 또는 anime여야 합니다.",
        },
      });
    }

    // 문자열 또는 null인지 검증
    const nullableStrings = {
      name_en,
      name_ja,
      profile_image,
      description,
      debut_date,
      official_url,
    };

    for (const [field, value] of Object.entries(nullableStrings)) {
      if (
        value !== null &&
        (typeof value !== "string" || value.trim() === "")
      ) {
        return res.status(400).json({
          error: {
            code: "INVALID_FIELD",
            message: `${field}는 문자열 또는 null이어야 합니다.`,
          },
        });
      }
    }

    // 날짜 검증
    if (
      debut_date !== null &&
      (!/^\d{4}-\d{2}-\d{2}$/.test(debut_date) ||
        Number.isNaN(Date.parse(`${debut_date}T00:00:00Z`)) ||
        new Date(`${debut_date}T00:00:00Z`).toISOString().slice(0, 10) !==
          debut_date)
    ) {
      return res.status(400).json({
        error: {
          code: "INVALID_DEBUT_DATE",
          message: "debut_date는 YYYY-MM-DD 형식의 올바른 날짜여야 합니다.",
        },
      });
    }

    // URL 검증
    for (const [field, value] of [
      ["profile_image", profile_image],
      ["official_url", official_url],
    ]) {
      if (value !== null) {
        try {
          const url = new URL(value);

          if (!["http:", "https:"].includes(url.protocol)) {
            throw new Error("Unsupported URL protocol");
          }
        } catch {
          return res.status(400).json({
            error: {
              code: "INVALID_URL",
              message: `${field}는 올바른 HTTP 또는 HTTPS URL이어야 합니다.`,
            },
          });
        }
      }
    }

    const artist = await registerArtist({
      name: name.trim(),
      name_en,
      name_ja,
      category,
      profile_image,
      description,
      debut_date,
      official_url,
    });

    return res.status(201).json({
      data: artist,
    });
  } catch (error) {
    console.error("아티스트 등록 오류:", error);

    return res.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "아티스트 등록 중 서버 오류가 발생했습니다.",
      },
    });
  }
}

const ARTIST_EDITABLE_FIELDS = new Set([
  "name",
  "name_en",
  "name_ja",
  "profile_image",
  "description",
  "debut_date",
  "official_url",
]);

export async function updateArtistController(req, res) {
  try {
    const artistId = Number(req.params.id);
    const body = req.body;

    if (!Number.isInteger(artistId) || artistId <= 0) {
      return res.status(400).json({
        error: {
          code: "INVALID_ARTIST_ID",
          message: "유효하지 않은 아티스트 ID입니다.",
        },
      });
    }

    if (!body || typeof body !== "object" || Array.isArray(body)) {
      return res.status(400).json({
        error: {
          code: "INVALID_BODY",
          message: "요청 본문은 객체여야 합니다.",
        },
      });
    }

    const entries = Object.entries(body);

    if (entries.length === 0) {
      return res.status(400).json({
        error: {
          code: "EMPTY_UPDATE",
          message: "수정할 필드를 하나 이상 보내야 합니다.",
        },
      });
    }

    // 허용된 필드인지 확인
    for (const [key] of entries) {
      if (!ARTIST_EDITABLE_FIELDS.has(key)) {
        return res.status(400).json({
          error: {
            code: "INVALID_FIELD",
            message: `수정할 수 없는 필드입니다: ${key}`,
          },
        });
      }
    }

    // 문자열 또는 null 검증
    for (const [key, value] of entries) {
      if (key === "name") {
        if (typeof value !== "string" || value.trim() === "") {
          return res.status(400).json({
            error: {
              code: "INVALID_NAME",
              message: "name은 비어 있지 않은 문자열이어야 합니다.",
            },
          });
        }
      } else if (key === "debut_date") {
        if (value !== null) {
          if (
            typeof value !== "string" ||
            !/^\d{4}-\d{2}-\d{2}$/.test(value) ||
            Number.isNaN(Date.parse(`${value}T00:00:00Z`)) ||
            new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) !== value
          ) {
            return res.status(400).json({
              error: {
                code: "INVALID_DEBUT_DATE",
                message:
                  "debut_date는 YYYY-MM-DD 형식의 날짜 또는 null이어야 합니다.",
              },
            });
          }
        }
      } else if (
        value !== null &&
        (typeof value !== "string" || value.trim() === "")
      ) {
        return res.status(400).json({
          error: {
            code: "INVALID_FIELD_VALUE",
            message: `${key}는 비어 있지 않은 문자열 또는 null이어야 합니다.`,
          },
        });
      }
    }

    // URL 검증
    for (const key of ["profile_image", "official_url"]) {
      const value = body[key];

      if (value !== undefined && value !== null) {
        try {
          const url = new URL(value);

          if (!["http:", "https:"].includes(url.protocol)) {
            throw new Error("Unsupported URL protocol");
          }
        } catch {
          return res.status(400).json({
            error: {
              code: "INVALID_URL",
              message: `${key}는 올바른 HTTP 또는 HTTPS URL이어야 합니다.`,
            },
          });
        }
      }
    }

    const fields = { ...body };

    if (typeof fields.name === "string") {
      fields.name = fields.name.trim();
    }

    const result = await editArtist({ artistId, fields });

    if (result === null) {
      return res.status(404).json({
        error: {
          code: "ARTIST_NOT_FOUND",
          message: "아티스트를 찾을 수 없습니다.",
        },
      });
    }

    if (result.error === "NO_FIELDS") {
      return res.status(400).json({
        error: {
          code: "EMPTY_UPDATE",
          message: "수정할 필드를 하나 이상 보내야 합니다.",
        },
      });
    }

    return res.status(200).json({
      data: result,
    });
  } catch (error) {
    console.error("아티스트 수정 오류:", error);

    return res.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "아티스트 수정 중 서버 오류가 발생했습니다.",
      },
    });
  }
}

const ALLOWED_ALBUM_TYPES = new Set(["album", "ep", "single"]);

export async function createArtistAlbumController(req, res) {
  try {
    const artistId = Number(req.params.id);
    const { title, album_type, release_date, cover_image, external_url } =
      req.body;

    if (!Number.isInteger(artistId) || artistId <= 0) {
      return res.status(400).json({
        error: {
          code: "INVALID_ARTIST_ID",
          message: "유효하지 않은 아티스트 ID입니다.",
        },
      });
    }

    if (typeof title !== "string" || title.trim() === "") {
      return res.status(400).json({
        error: {
          code: "INVALID_TITLE",
          message: "앨범 제목은 필수입니다.",
        },
      });
    }

    if (!ALLOWED_ALBUM_TYPES.has(album_type)) {
      return res.status(400).json({
        error: {
          code: "INVALID_ALBUM_TYPE",
          message: "album_type은 album, ep, single 중 하나여야 합니다.",
        },
      });
    }

    // 선택 필드는 문자열 또는 null이어야 함
    const optionalFields = {
      release_date,
      cover_image,
      external_url,
    };

    for (const [field, value] of Object.entries(optionalFields)) {
      if (value === undefined) {
        return res.status(400).json({
          error: {
            code: "MISSING_FIELD",
            message: `${field} 필드를 포함해야 합니다. 값이 없다면 null을 보내세요.`,
          },
        });
      }

      if (
        value !== null &&
        (typeof value !== "string" || value.trim() === "")
      ) {
        return res.status(400).json({
          error: {
            code: "INVALID_FIELD",
            message: `${field}는 문자열 또는 null이어야 합니다.`,
          },
        });
      }
    }

    // 날짜 형식 및 실제 날짜 검증
    if (release_date !== null) {
      const validDate =
        /^\d{4}-\d{2}-\d{2}$/.test(release_date) &&
        !Number.isNaN(Date.parse(`${release_date}T00:00:00Z`)) &&
        new Date(`${release_date}T00:00:00Z`).toISOString().slice(0, 10) ===
          release_date;

      if (!validDate) {
        return res.status(400).json({
          error: {
            code: "INVALID_RELEASE_DATE",
            message:
              "release_date는 YYYY-MM-DD 형식의 날짜 또는 null이어야 합니다.",
          },
        });
      }
    }

    // 이미지 및 외부 링크 URL 검증
    for (const [field, value] of [
      ["cover_image", cover_image],
      ["external_url", external_url],
    ]) {
      if (value !== null) {
        try {
          const url = new URL(value);

          if (!["http:", "https:"].includes(url.protocol)) {
            throw new Error("Unsupported URL protocol");
          }
        } catch {
          return res.status(400).json({
            error: {
              code: "INVALID_URL",
              message: `${field}는 올바른 HTTP 또는 HTTPS URL이어야 합니다.`,
            },
          });
        }
      }
    }

    const album = await registerArtistAlbum({
      artistId,
      title: title.trim(),
      album_type,
      release_date,
      cover_image,
      external_url,
    });

    if (!album) {
      return res.status(404).json({
        error: {
          code: "ARTIST_NOT_FOUND",
          message: "아티스트를 찾을 수 없습니다.",
        },
      });
    }

    return res.status(201).json({
      data: album,
    });
  } catch (error) {
    console.error("앨범 등록 오류:", error);

    return res.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "앨범 등록 중 서버 오류가 발생했습니다.",
      },
    });
  }
}

export async function createArtistSongController(req, res) {
  try {
    const artistId = Number(req.params.id);

    if (!Number.isInteger(artistId) || artistId <= 0) {
      return res.status(400).json({
        message: "올바른 아티스트 ID가 아닙니다.",
      });
    }

    const {
      title,
      album_id,
      track_no,
      release_date,
      is_representative,
      external_url,
    } = req.body;

    // 필수 필드 확인
    const requiredFields = [
      "title",
      "album_id",
      "track_no",
      "release_date",
      "is_representative",
      "external_url",
    ];

    const missingFields = requiredFields.filter(
      (field) => !Object.prototype.hasOwnProperty.call(req.body, field),
    );

    if (missingFields.length > 0) {
      return res.status(400).json({
        message: "필수 필드가 누락되었습니다.",
        fields: missingFields,
      });
    }

    // 곡 제목
    if (typeof title !== "string" || title.trim().length === 0) {
      return res.status(400).json({
        message: "곡 제목은 비어 있을 수 없습니다.",
      });
    }

    // 앨범 ID
    if (album_id !== null && (!Number.isInteger(album_id) || album_id <= 0)) {
      return res.status(400).json({
        message: "album_id는 양의 정수 또는 null이어야 합니다.",
      });
    }

    // 트랙 번호
    if (track_no !== null && (!Number.isInteger(track_no) || track_no <= 0)) {
      return res.status(400).json({
        message: "track_no는 양의 정수 또는 null이어야 합니다.",
      });
    }

    // 발매일
    if (release_date !== null) {
      if (
        typeof release_date !== "string" ||
        !/^\d{4}-\d{2}-\d{2}$/.test(release_date)
      ) {
        return res.status(400).json({
          message: "release_date는 YYYY-MM-DD 형식이어야 합니다.",
        });
      }

      const date = new Date(`${release_date}T00:00:00Z`);

      if (
        Number.isNaN(date.getTime()) ||
        date.toISOString().slice(0, 10) !== release_date
      ) {
        return res.status(400).json({
          message: "유효하지 않은 발매일입니다.",
        });
      }
    }

    // 대표곡 여부
    if (typeof is_representative !== "boolean") {
      return res.status(400).json({
        message: "is_representative는 boolean이어야 합니다.",
      });
    }

    // 외부 URL
    if (external_url !== null) {
      if (typeof external_url !== "string") {
        return res.status(400).json({
          message: "external_url은 문자열 또는 null이어야 합니다.",
        });
      }

      try {
        const url = new URL(external_url);

        if (!["http:", "https:"].includes(url.protocol)) {
          throw new Error("Unsupported protocol");
        }
      } catch {
        return res.status(400).json({
          message: "유효한 HTTP 또는 HTTPS URL을 입력해 주세요.",
        });
      }
    }

    const song = await registerArtistSong({
      artistId,
      title: title.trim(),
      album_id,
      track_no,
      release_date,
      is_representative,
      external_url,
    });

    if (song.error === "ARTIST_NOT_FOUND") {
      return res.status(404).json({
        message: "아티스트를 찾을 수 없습니다.",
      });
    }

    if (song.error === "INVALID_ALBUM") {
      return res.status(400).json({
        message: "앨범이 존재하지 않거나 해당 아티스트의 앨범이 아닙니다.",
      });
    }

    return res.status(201).json({
      data: {
        id: song.id,
      },
    });
  } catch (error) {
    console.error("곡 등록 실패:", error);

    return res.status(500).json({
      message: "서버 오류가 발생했습니다.",
    });
  }
}

const EVENT_CATEGORIES = ["jpop", "anime"];

const EVENT_TYPES = [
  "concert",
  "popup",
  "collabo_cafe",
  "fair",
  "exhibition",
  "release",
  "etc",
];

function isValidUrl(value) {
  if (typeof value !== "string") return false;

  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol);
  } catch {
    return false;
  }
}

function isValidDateTime(value) {
  if (typeof value !== "string") return false;

  // 시간대가 명시된 ISO 8601 형식인지 확인
  const hasTimezone =
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?(Z|[+-]\d{2}:\d{2})$/.test(
      value,
    );

  return hasTimezone && !Number.isNaN(Date.parse(value));
}

function isValidNullableId(value) {
  return value === null || (Number.isInteger(value) && value > 0);
}

function isValidNullableNumber(value) {
  return (
    value === null || (typeof value === "number" && Number.isFinite(value))
  );
}

function isValidNullableString(value) {
  return value === null || typeof value === "string";
}

export async function createAdminEventController(req, res) {
  try {
    const {
      category,
      type,
      title,
      artist_id,
      anime_id,
      venue,
      address,
      city,
      lat,
      lng,
      start_at,
      end_at,
      is_all_day,
      reservation_required,
      poster_url,
      ticket_url,
      source_url,
      processed_item_id,
    } = req.body;

    // 필수 필드 누락 확인
    const requiredFields = [
      "category",
      "type",
      "title",
      "artist_id",
      "anime_id",
      "venue",
      "address",
      "city",
      "lat",
      "lng",
      "start_at",
      "end_at",
      "is_all_day",
      "reservation_required",
      "poster_url",
      "ticket_url",
      "source_url",
      "processed_item_id",
    ];

    const missingFields = requiredFields.filter(
      (field) => !Object.prototype.hasOwnProperty.call(req.body, field),
    );

    if (missingFields.length > 0) {
      return res.status(400).json({
        message: "필수 필드가 누락되었습니다.",
        fields: missingFields,
      });
    }

    if (!EVENT_CATEGORIES.includes(category)) {
      return res.status(400).json({
        message: "category 값이 올바르지 않습니다.",
      });
    }

    if (!EVENT_TYPES.includes(type)) {
      return res.status(400).json({
        message: "type 값이 올바르지 않습니다.",
      });
    }

    if (typeof title !== "string" || !title.trim()) {
      return res.status(400).json({
        message: "title은 비어 있을 수 없습니다.",
      });
    }

    if (typeof venue !== "string" || !venue.trim()) {
      return res.status(400).json({
        message: "venue는 비어 있을 수 없습니다.",
      });
    }

    if (
      !isValidNullableId(artist_id) ||
      !isValidNullableId(anime_id) ||
      !isValidNullableId(processed_item_id)
    ) {
      return res.status(400).json({
        message: "ID 필드는 양의 정수 또는 null이어야 합니다.",
      });
    }

    if (!isValidNullableString(address) || !isValidNullableString(city)) {
      return res.status(400).json({
        message: "address와 city는 문자열 또는 null이어야 합니다.",
      });
    }

    if (!isValidNullableNumber(lat) || !isValidNullableNumber(lng)) {
      return res.status(400).json({
        message: "lat와 lng는 숫자 또는 null이어야 합니다.",
      });
    }

    if (
      (lat !== null && (lat < -90 || lat > 90)) ||
      (lng !== null && (lng < -180 || lng > 180))
    ) {
      return res.status(400).json({
        message: "위도 또는 경도 범위가 올바르지 않습니다.",
      });
    }

    if (!isValidDateTime(start_at)) {
      return res.status(400).json({
        message: "start_at은 시간대가 포함된 ISO 8601 형식이어야 합니다.",
      });
    }

    if (end_at !== null) {
      if (!isValidDateTime(end_at)) {
        return res.status(400).json({
          message:
            "end_at은 시간대가 포함된 ISO 8601 형식 또는 null이어야 합니다.",
        });
      }

      if (Date.parse(end_at) < Date.parse(start_at)) {
        return res.status(400).json({
          message: "end_at은 start_at보다 빠를 수 없습니다.",
        });
      }
    }

    if (typeof is_all_day !== "boolean") {
      return res.status(400).json({
        message: "is_all_day는 boolean이어야 합니다.",
      });
    }

    if (
      reservation_required !== null &&
      typeof reservation_required !== "boolean"
    ) {
      return res.status(400).json({
        message: "reservation_required는 boolean 또는 null이어야 합니다.",
      });
    }

    for (const [field, value] of [
      ["poster_url", poster_url],
      ["ticket_url", ticket_url],
    ]) {
      if (value !== null && !isValidUrl(value)) {
        return res.status(400).json({
          message: `${field}는 유효한 HTTP 또는 HTTPS URL이어야 합니다.`,
        });
      }
    }

    if (!isValidUrl(source_url)) {
      return res.status(400).json({
        message: "source_url은 유효한 HTTP 또는 HTTPS URL이어야 합니다.",
      });
    }

    const event = await registerAdminEvent({
      eventData: {
        category,
        type,
        title: title.trim(),
        artist_id,
        anime_id,
        venue: venue.trim(),
        address,
        city,
        lat,
        lng,
        start_at,
        end_at,
        is_all_day,
        reservation_required,
        poster_url,
        ticket_url,
        source_url,
        processed_item_id,
      },
      adminId: req.user.id,
    });

    if (event.error === "CANDIDATE_NOT_FOUND") {
      return res.status(400).json({
        message: "등록할 수집 후보를 찾을 수 없습니다.",
      });
    }

    return res.status(201).json({
      data: {
        id: event.id,
        created_at: event.created_at,
      },
    });
  } catch (error) {
    console.error("관리자 이벤트 등록 실패:", error);

    return res.status(500).json({
      message: "서버 오류가 발생했습니다.",
    });
  }
}

export async function deleteAdminEventController(req, res) {
  try {
    const eventId = Number(req.params.id);

    // ID 검증
    if (!Number.isInteger(eventId) || eventId <= 0) {
      return res.status(400).json({
        message: "올바른 이벤트 ID가 아닙니다.",
      });
    }

    const result = await removeAdminEvent(eventId);

    // 이벤트가 존재하지 않는 경우
    if (!result) {
      return res.status(404).json({
        message: "이벤트를 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({
      deleted: true,
    });
  } catch (error) {
    console.error("관리자 이벤트 삭제 실패:", error);

    return res.status(500).json({
      message: "서버 오류가 발생했습니다.",
    });
  }
}

const ALLOWED_EVENT_STATUSES = ["upcoming", "ongoing", "ended", "canceled"];

const ALLOWED_EVENT_FIELDS = [
  "title",
  "venue",
  "address",
  "city",
  "lat",
  "lng",
  "start_at",
  "end_at",
  "status",
  "source_url",
];

function isValidEventUrl(value) {
  if (typeof value !== "string") return false;

  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol);
  } catch {
    return false;
  }
}

function isValidEventDateTime(value) {
  if (typeof value !== "string") return false;

  const hasTimezone =
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?(Z|[+-]\d{2}:\d{2})$/.test(
      value,
    );

  return hasTimezone && !Number.isNaN(Date.parse(value));
}

export async function updateAdminEventController(req, res) {
  try {
    const eventId = Number(req.params.id);

    if (!Number.isInteger(eventId) || eventId <= 0) {
      return res.status(400).json({
        message: "올바른 이벤트 ID가 아닙니다.",
      });
    }

    const body = req.body ?? {};
    const keys = Object.keys(body);

    if (keys.length === 0) {
      return res.status(400).json({
        message: "수정할 필드를 입력해 주세요.",
      });
    }

    const invalidFields = keys.filter(
      (key) => !ALLOWED_EVENT_FIELDS.includes(key),
    );

    if (invalidFields.length > 0) {
      return res.status(400).json({
        message: "수정할 수 없는 필드가 포함되어 있습니다.",
        fields: invalidFields,
      });
    }

    const fields = {};

    for (const [key, value] of Object.entries(body)) {
      switch (key) {
        case "title":
        case "venue":
          if (typeof value !== "string" || !value.trim()) {
            return res.status(400).json({
              message: `${key}는 비어 있지 않은 문자열이어야 합니다.`,
            });
          }
          fields[key] = value.trim();
          break;

        case "address":
        case "city":
          if (value !== null && typeof value !== "string") {
            return res.status(400).json({
              message: `${key}는 문자열 또는 null이어야 합니다.`,
            });
          }
          fields[key] = value;
          break;

        case "lat":
          if (
            value !== null &&
            (typeof value !== "number" ||
              !Number.isFinite(value) ||
              value < -90 ||
              value > 90)
          ) {
            return res.status(400).json({
              message: "lat는 -90부터 90 사이의 숫자 또는 null이어야 합니다.",
            });
          }
          fields[key] = value;
          break;

        case "lng":
          if (
            value !== null &&
            (typeof value !== "number" ||
              !Number.isFinite(value) ||
              value < -180 ||
              value > 180)
          ) {
            return res.status(400).json({
              message: "lng는 -180부터 180 사이의 숫자 또는 null이어야 합니다.",
            });
          }
          fields[key] = value;
          break;

        case "start_at":
        case "end_at":
          if (value !== null && !isValidEventDateTime(value)) {
            return res.status(400).json({
              message: `${key}는 시간대가 포함된 ISO 8601 형식이어야 합니다.`,
            });
          }

          if (key === "start_at" && value === null) {
            return res.status(400).json({
              message: "start_at은 null일 수 없습니다.",
            });
          }

          fields[key] = value;
          break;

        case "status":
          if (!ALLOWED_EVENT_STATUSES.includes(value)) {
            return res.status(400).json({
              message: "status 값이 올바르지 않습니다.",
            });
          }
          fields[key] = value;
          break;

        case "source_url":
          if (!isValidEventUrl(value)) {
            return res.status(400).json({
              message: "source_url은 유효한 HTTP 또는 HTTPS URL이어야 합니다.",
            });
          }
          fields[key] = value;
          break;
      }
    }

    // 시작일과 종료일을 함께 수정하거나, 한쪽만 수정하는 경우도 검증
    if (
      fields.start_at !== undefined &&
      fields.end_at !== undefined &&
      fields.end_at !== null &&
      Date.parse(fields.end_at) < Date.parse(fields.start_at)
    ) {
      return res.status(400).json({
        message: "end_at은 start_at보다 빠를 수 없습니다.",
      });
    }

    const result = await editAdminEvent({
      eventId,
      fields,
    });

    if (result.error === "NO_FIELDS") {
      return res.status(400).json({
        message: "수정할 필드를 입력해 주세요.",
      });
    }

    if (!result) {
      return res.status(404).json({
        message: "이벤트를 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({
      data: {
        id: result.id,
        updated_at: result.updated_at,
      },
    });
  } catch (error) {
    console.error("관리자 이벤트 수정 실패:", error);

    return res.status(500).json({
      message: "서버 오류가 발생했습니다.",
    });
  }
}

export async function getAdminFollowsController(req, res) {
  try {
    const {
      followType,
      matchStatus,
      page: pageQuery,
      limit: limitQuery,
    } = req.query;

    // 팔로우 종류 검증
    if (followType !== undefined && !["artist", "anime"].includes(followType)) {
      return res.status(400).json({
        message: "followType은 artist 또는 anime이어야 합니다.",
      });
    }

    // 매칭 상태 검증
    if (
      matchStatus !== undefined &&
      !["pending", "unmatched"].includes(matchStatus)
    ) {
      return res.status(400).json({
        message: "matchStatus는 pending 또는 unmatched이어야 합니다.",
      });
    }

    // 페이지 번호 검증
    const page = pageQuery === undefined ? 1 : Number(pageQuery);
    const limit = limitQuery === undefined ? 20 : Number(limitQuery);

    if (!Number.isInteger(page) || page < 1) {
      return res.status(400).json({
        message: "page는 1 이상의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(limit) || limit < 1 || limit > 100) {
      return res.status(400).json({
        message: "limit은 1부터 100 사이의 정수여야 합니다.",
      });
    }

    const result = await getAdminFollowRequests({
      followType,
      matchStatus,
      page,
      limit,
    });

    return res.status(200).json({
      data: result.data,
      meta: {
        page,
        total: result.total,
      },
    });
  } catch (error) {
    console.error("관리자 팔로우 요청 조회 실패:", error);

    return res.status(500).json({
      message: "서버 오류가 발생했습니다.",
    });
  }
}

export async function matchAdminFollowController(req, res) {
  try {
    const { followType } = req.params;
    const followId = Number(req.params.followId);

    if (!["artist", "anime"].includes(followType)) {
      return res.status(400).json({
        message: "followType은 artist 또는 anime이어야 합니다.",
      });
    }

    if (!Number.isInteger(followId) || followId <= 0) {
      return res.status(400).json({
        message: "올바른 followId가 아닙니다.",
      });
    }

    const { target_id, match_status, apply_same_input } = req.body ?? {};

    const requiredFields = ["target_id", "match_status", "apply_same_input"];

    const missingFields = requiredFields.filter(
      (field) => !Object.prototype.hasOwnProperty.call(req.body ?? {}, field),
    );

    if (missingFields.length > 0) {
      return res.status(400).json({
        message: "필수 필드가 누락되었습니다.",
        fields: missingFields,
      });
    }

    if (!["matched", "unmatched"].includes(match_status)) {
      return res.status(400).json({
        message: "match_status는 matched 또는 unmatched이어야 합니다.",
      });
    }

    if (typeof apply_same_input !== "boolean") {
      return res.status(400).json({
        message: "apply_same_input은 boolean이어야 합니다.",
      });
    }

    if (
      match_status === "matched" &&
      (!Number.isInteger(target_id) || target_id <= 0)
    ) {
      return res.status(400).json({
        message: "matched 상태에서는 target_id가 양의 정수여야 합니다.",
      });
    }

    if (match_status === "unmatched" && target_id !== null) {
      return res.status(400).json({
        message: "unmatched 상태에서는 target_id를 null로 보내야 합니다.",
      });
    }

    const result = await resolveAdminFollow({
      followType,
      followId,
      targetId: target_id,
      matchStatus: match_status,
      applySameInput: apply_same_input,
    });

    if (result.error === "FOLLOW_NOT_FOUND") {
      return res.status(404).json({
        message: "팔로우 요청을 찾을 수 없습니다.",
      });
    }

    if (result.error === "TARGET_NOT_FOUND") {
      return res.status(400).json({
        message: "연결할 아티스트 또는 애니메이션을 찾을 수 없습니다.",
      });
    }

    if (result.error === "FOLLOW_ALREADY_PROCESSED") {
      return res.status(409).json({
        message: "이미 처리된 팔로우 요청입니다.",
      });
    }

    if (result.updated_count === 0) {
      return res.status(409).json({
        message: "처리할 미매칭 요청이 없습니다.",
      });
    }

    return res.status(200).json({
      data: {
        updated_count: result.updated_count,
      },
    });
  } catch (error) {
    console.error("관리자 팔로우 매칭 실패:", error);

    return res.status(500).json({
      message: "서버 오류가 발생했습니다.",
    });
  }
}
