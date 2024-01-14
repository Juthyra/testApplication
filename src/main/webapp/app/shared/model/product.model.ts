export interface IProduct {
  id?: number;
  bookTitle?: string | null;
  bookPrice?: number | null;
  bookQuantity?: number | null;
}

export const defaultValue: Readonly<IProduct> = {};
