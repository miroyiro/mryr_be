import { Router } from "express";
import {
  signupController,
  loginController,
  logoutController,
  meController,
  refreshController,
  oauthStartController,
  oauthCallbackController,
} from "../controllers/auth.controller.js";
import { authMiddleware } from "../middlewares/auth.middleware.js";

const router = Router();

// 기존 라우트
router.post("/signup", signupController);
router.post("/login", loginController);
router.post("/logout", authMiddleware, logoutController);
router.get("/me", authMiddleware, meController);
router.post("/refresh", refreshController);

// 소셜 로그인
router.get("/:provider/callback", oauthCallbackController);
router.get("/:provider", oauthStartController);

export default router;
