import { Role } from "../../models/role";
export interface UserResponse {
    id: number;
    fullname: string;
    username?: string;
    email?: string;
    phone?: string;
    address?: string | null;
    avatar?: string;
    gender?: string;
    dob?: string; // Date of birth from API
    date_of_birth?: Date; // Legacy field
    is_active?: boolean; // Legacy field
    status?: string; // ACTIVE, INACTIVE, etc.
    create_date?: string; // Member since date (ISO string from API)
    facebook_account_id?: number;
    google_account_id?: number;
    role?: Role | string; // Can be Role object or string like "GUEST"
    // User statistics (all can be null from API)
    total_bookings?: number | null;
    total_reviews?: number | null;
    favorite_properties_count?: number | null;
    active_promotions_count?: number | null;
    hosted_properties_count?: number | null;
    total_earnings?: number | null;
    average_rating?: number | null;
    total_property_reviews?: number | null;
}