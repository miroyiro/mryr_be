import { getEvents, getEventDetail } from "../services/events.service.js";

function isValidDate(value) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const date = new Date(`${value}T00:00:00.000Z`);
  return (
    !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value
  );
}

export async function getEventsController(req, res, next) {
  try {
    const { category, type, status, animeId, artistId, city, from, to } =
      req.query;

    const page = req.query.page === undefined ? 1 : Number(req.query.page);

    const limit = req.query.limit === undefined ? 10 : Number(req.query.limit);

    if (!Number.isInteger(page) || page < 1) {
      return res.status(400).json({
        message: "page는 1 이상의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
      return res.status(400).json({
        message: "limit은 1~50 사이의 정수여야 합니다.",
      });
    }

    for (const [name, value] of Object.entries({
      category,
      type,
      status,
      city,
    })) {
      if (value !== undefined && (typeof value !== "string" || !value.trim())) {
        return res.status(400).json({
          message: `${name}은 비어 있지 않은 문자열이어야 합니다.`,
        });
      }
    }

    let parsedAnimeId;
    let parsedArtistId;

    for (const [name, value] of [
      ["animeId", animeId],
      ["artistId", artistId],
    ]) {
      if (value !== undefined) {
        const parsed = Number(value);

        if (!Number.isInteger(parsed) || parsed < 1) {
          return res.status(400).json({
            message: `${name}는 양의 정수여야 합니다.`,
          });
        }

        if (name === "animeId") parsedAnimeId = parsed;
        else parsedArtistId = parsed;
      }
    }

    if (from !== undefined && !isValidDate(from)) {
      return res.status(400).json({
        message: "from은 유효한 YYYY-MM-DD 날짜여야 합니다.",
      });
    }

    if (to !== undefined && !isValidDate(to)) {
      return res.status(400).json({
        message: "to는 유효한 YYYY-MM-DD 날짜여야 합니다.",
      });
    }

    if (from && to && from > to) {
      return res.status(400).json({
        message: "from은 to보다 늦을 수 없습니다.",
      });
    }

    const result = await getEvents({
      category: category?.trim(),
      type: type?.trim(),
      status: status?.trim(),
      animeId: parsedAnimeId,
      artistId: parsedArtistId,
      city: city?.trim(),
      from,
      to,
      page,
      limit,
    });

    return res.status(200).json({
      data: result.data,
      meta: {
        page,
        limit,
        total: result.total,
        total_pages: Math.ceil(result.total / limit),
      },
    });
  } catch (error) {
    next(error);
  }
}

export async function getEventDetailController(req, res, next) {
  try {
    const eventId = Number(req.params.id);

    if (!Number.isInteger(eventId) || eventId < 1) {
      return res.status(400).json({
        message: "이벤트 ID는 양의 정수여야 합니다.",
      });
    }

    const data = await getEventDetail({
      eventId,
      userId: req.user?.id ?? null,
    });

    if (!data) {
      return res.status(404).json({
        message: "이벤트를 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}
