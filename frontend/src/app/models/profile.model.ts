export interface ProfileModel {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  roleId: number | null;
  roleName: string;
}
