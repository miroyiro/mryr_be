import { Router } from "express";
import {
  getHomeFeedController,
  getGuestHomeFeedController,
  getPopularHomeFeedController,
  saveOnboardingFollowController,
} from "../controllers/home.controller.js";
import { authMiddleware } from "../middlewares/auth.middleware.js";

const router = Router();

//로그인한 사용자의 소식 피드
router.get("/feed", authMiddleware, getHomeFeedController);
//비로그인 사용자의 이벤트 피드
router.get("/feed/guest", getGuestHomeFeedController);
//인기 이벤트 조회
router.get("/feed/popular", getPopularHomeFeedController);
//최초 관심사 저장
router.post(
  "/onboarding/follow",
  authMiddleware,
  saveOnboardingFollowController,
);

export default router;
