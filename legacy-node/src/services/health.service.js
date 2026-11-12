const getHealth = async () => {
  return {
    success: true,
    message: "Server is healthy",
  };
};

export default {
  getHealth,
};
