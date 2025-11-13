import {
    IsString, 
    IsNotEmpty
} from 'class-validator';

export class LoginDTO {
    @IsString()
    @IsNotEmpty()
    usernameOrEmail: string;

    @IsString()
    @IsNotEmpty()
    password: string;

    // NOTE: Optional field - backend uses this to set RT cookie MaxAge
    rememberMe?: boolean;

    constructor(data: any) {
        this.usernameOrEmail = data.usernameOrEmail;
        this.password = data.password;
        this.rememberMe = data.rememberMe;
    }
}