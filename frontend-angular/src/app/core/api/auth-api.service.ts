import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { UserRole, UserSession, demoUsers } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  login(role: UserRole): Observable<UserSession> {
    const user = demoUsers.find((candidate) => candidate.role === role) ?? demoUsers[0];
    return of(user);
  }
}
