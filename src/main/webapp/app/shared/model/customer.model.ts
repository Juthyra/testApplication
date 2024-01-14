export interface ICustomer {
  id?: number;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  familyMember?: number | null;
}

export const defaultValue: Readonly<ICustomer> = {};
