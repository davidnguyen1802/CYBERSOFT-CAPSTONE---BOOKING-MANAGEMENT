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

    constructor(data: any) {
        this.usernameOrEmail = data.usernameOrEmail;
        this.password = data.password;
    }
}