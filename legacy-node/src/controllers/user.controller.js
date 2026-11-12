import { getMyStreak } from "../services/user.service.js";

export async function getMyStreakController(req, res, next) {
  try {
    const rawYear = req.query.year;
    const rawMonth = req.query.month;

    // 연도와 월은 함께 전달하거나 둘 다 생략
    if ((rawYear === undefined) !== (rawMonth === undefined)) {
      const error = new Error("year와 month는 함께 입력해야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    let year;
    let month;

    if (rawYear !== undefined) {
      if (
        typeof rawYear !== "string" ||
        !/^\d{4}$/.test(rawYear) ||
        typeof rawMonth !== "string" ||
        !/^\d{1,2}$/.test(rawMonth)
      ) {
        const error = new Error("year 또는 month 형식이 올바르지 않습니다.");
        error.statusCode = 400;
        throw error;
      }

      year = Number(rawYear);
      month = Number(rawMonth);

      if (year < 2000 || year > 2100 || month < 1 || month > 12) {
        const error = new Error("year 또는 month의 범위가 올바르지 않습니다.");
        error.statusCode = 400;
        throw error;
      }
    }

    const data = await getMyStreak({
      userId: req.user.id,
      year,
      month,
    });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}
