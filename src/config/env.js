import dotenv from "dotenv";

dotenv.config();
console.log("DB 사용자:", process.env.DB_USER);
console.log("DB 비밀번호:", process.env.DB_PASSWORD);

const env = {
  port: Number(process.env.PORT),

  db: {
    host: process.env.DB_HOST,
    port: Number(process.env.DB_PORT),
    name: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
  },
};

export default env;
