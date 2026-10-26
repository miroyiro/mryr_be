import cron from "node-cron";
import { savePopularKeywordSnapshot } from "../repositories/search.repository.js";

export function startPopularSearchJob() {
  // 서버 시작 시 최초 집계
  savePopularKeywordSnapshot().catch((error) => {
    console.error("인기 검색어 초기 집계 실패:", error);
  });

  // 매시간 정각에 집계
  cron.schedule("0 * * * *", async () => {
    try {
      await savePopularKeywordSnapshot();
      console.log("인기 검색어 스냅샷 저장 완료");
    } catch (error) {
      console.error("인기 검색어 스냅샷 저장 실패:", error);
    }
  }, {
    timezone: "Asia/Seoul",
  });
}
