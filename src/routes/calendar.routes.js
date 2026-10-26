import { Router } from "express";
import { authMiddleware } from "../middlewares/auth.middleware.js";
import {
  createCalendarController,
  updateCalendarController,
  deleteCalendarController,
  getMonthlyAnimeCalendarController,
  getDailyAnimeCalendarController,
  getUpcomingAnimeCalendarController,
  getMonthlyJpopCalendarController,
  getDailyJpopCalendarController,
  getUpcomingJpopCalendarController,
} from "../controllers/calendar.controller.js";

const router = Router();

// 내 캘린더에 일정 등록
router.post("/", authMiddleware, createCalendarController);
// 캘린더 일정 수정
router.patch("/:calendarId", authMiddleware, updateCalendarController);
//캘린더 일정 삭제
router.delete("/:calendarId", authMiddleware, deleteCalendarController);
// 월별 애니메이션 일정 조회
router.get("/anime", authMiddleware, getMonthlyAnimeCalendarController);
// 정적 경로를 날짜 파라미터 경로보다 먼저 등록
// 다가오는 애니메이션 일정 조회
router.get(
  "/anime/upcoming",
  authMiddleware,
  getUpcomingAnimeCalendarController,
);
// 일별 애니메이션 일정 조회
router.get("/anime/:date", authMiddleware, getDailyAnimeCalendarController);
// 월별 J-POP 일정 조회
router.get("/jpop", authMiddleware, getMonthlyJpopCalendarController);
// 다가오는 J-POP 일정 조회
router.get("/jpop/upcoming", authMiddleware, getUpcomingJpopCalendarController);
// 일별 J-POP 일정 조회
router.get("/jpop/:date", authMiddleware, getDailyJpopCalendarController);
export default router;
