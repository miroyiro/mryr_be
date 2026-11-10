import cron from "node-cron";

export function startStreakCloseJob() {
  cron.schedule(
    "0 0 * * *",
    async () => {
      // TODO:
      // 1. 어제의 user_activity_log 조회
      // 2. 확정된 점수 규칙으로 사용자별 점수 계산
      // 3. 하루 최대 5점 적용
      // 4. streak 테이블에 사용자별 점수 저장
    },
    { timezone: "Asia/Seoul" },
  );
}
