import {
  getArtistDetail,
  getArtistAlbums,
  followArtist,
  unfollowArtist,
  getArtistSongs,
} from "../services/artist.service.js";

export async function getArtistDetailController(req, res, next) {
  try {
    const artistId = Number(req.params.id);

    if (!Number.isInteger(artistId) || artistId < 1) {
      return res.status(400).json({
        message: "아티스트 ID는 양의 정수여야 합니다.",
      });
    }

    const data = await getArtistDetail({ artistId });

    if (data === null) {
      return res.status(404).json({
        message: "아티스트를 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getArtistAlbumsController(req, res, next) {
  try {
    const artistId = Number(req.params.id);
    const page = Number(req.query.page ?? 1);
    const limit = Number(req.query.limit ?? 20);

    if (!Number.isInteger(artistId) || artistId < 1) {
      return res.status(400).json({
        message: "아티스트 ID는 양의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(page) || page < 1) {
      return res.status(400).json({
        message: "page는 1 이상의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
      return res.status(400).json({
        message: "limit는 1부터 50 사이의 정수여야 합니다.",
      });
    }

    const result = await getArtistAlbums({
      artistId,
      page,
      limit,
    });

    if (result === null) {
      return res.status(404).json({
        message: "아티스트를 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({
      data: result.albums,
      pagination: {
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

export async function followArtistController(req, res) {
  try {
    const artistId = Number(req.params.id);
    const userId = req.user.id;

    // 아티스트 ID 검증
    if (!Number.isInteger(artistId) || artistId <= 0) {
      return res.status(400).json({
        error: {
          code: "INVALID_ARTIST_ID",
          message: "유효하지 않은 아티스트 ID입니다.",
        },
      });
    }

    const result = await followArtist({ artistId, userId });

    // 존재하지 않는 아티스트
    if (!result) {
      return res.status(404).json({
        error: {
          code: "ARTIST_NOT_FOUND",
          message: "아티스트를 찾을 수 없습니다.",
        },
      });
    }

    const { created, ...data } = result;

    return res.status(created ? 201 : 200).json({ data });
  } catch (error) {
    console.error("아티스트 팔로우 오류:", error);

    return res.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "서버 내부 오류가 발생했습니다.",
      },
    });
  }
}

export async function unfollowArtistController(req, res) {
  try {
    const artistId = Number(req.params.id);
    const userId = req.user.id;

    // 아티스트 ID 검증
    if (!Number.isInteger(artistId) || artistId <= 0) {
      return res.status(400).json({
        error: {
          code: "INVALID_ARTIST_ID",
          message: "유효하지 않은 아티스트 ID입니다.",
        },
      });
    }

    const result = await unfollowArtist({ artistId, userId });

    // 아티스트가 존재하지 않는 경우
    if (!result) {
      return res.status(404).json({
        error: {
          code: "ARTIST_NOT_FOUND",
          message: "아티스트를 찾을 수 없습니다.",
        },
      });
    }

    return res.status(200).json({
      data: result,
    });
  } catch (error) {
    console.error("아티스트 언팔로우 오류:", error);

    return res.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "서버 내부 오류가 발생했습니다.",
      },
    });
  }
}

export async function getArtistSongsController(req, res) {
  try {
    const artistId = Number(req.params.id);
    const page = Number(req.query.page ?? 1);
    const limit = Number(req.query.limit ?? 20);

    // 아티스트 ID 검증
    if (!Number.isInteger(artistId) || artistId <= 0) {
      return res.status(400).json({
        error: {
          code: "INVALID_ARTIST_ID",
          message: "유효하지 않은 아티스트 ID입니다.",
        },
      });
    }

    // 페이지네이션 검증
    if (
      !Number.isInteger(page) ||
      page < 1 ||
      !Number.isInteger(limit) ||
      limit < 1 ||
      limit > 50
    ) {
      return res.status(400).json({
        error: {
          code: "INVALID_PAGINATION",
          message: "page는 1 이상, limit은 1~50 사이의 정수여야 합니다.",
        },
      });
    }

    const result = await getArtistSongs({
      artistId,
      page,
      limit,
    });

    if (!result) {
      return res.status(404).json({
        error: {
          code: "ARTIST_NOT_FOUND",
          message: "아티스트를 찾을 수 없습니다.",
        },
      });
    }

    return res.status(200).json({
      data: result.songs,
      pagination: {
        page,
        limit,
        total: result.total,
        total_pages: Math.ceil(result.total / limit),
      },
    });
  } catch (error) {
    console.error("아티스트 곡 목록 조회 오류:", error);

    return res.status(500).json({
      error: {
        code: "INTERNAL_SERVER_ERROR",
        message: "서버 내부 오류가 발생했습니다.",
      },
    });
  }
}
