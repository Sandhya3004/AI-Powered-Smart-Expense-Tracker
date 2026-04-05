import api from "./api";

export const login = async (data) => {
  const response = await api.post("/auth/login", data);
  return response.data;
};

export const register = async (data) => {
  await api.post("/auth/register", data);
};

export const getProfile = async () => {
  const response = await api.get("/auth/profile");
  return response.data;
};

export const logout = () => {
  localStorage.removeItem("token");
  delete api.defaults.headers.common["Authorization"];
};
