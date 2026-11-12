import healthService from "../services/health.service.js";

const getHealth = async (req, res) => {
  const result = await healthService.getHealth();

  res.status(200).json(result);
};

export default {
  getHealth,
};
