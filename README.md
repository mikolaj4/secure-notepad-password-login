# AES-secured-notepad

Głównym celem projektu jest zastosowanie jak najlepszych mechanizmów szyfrujących do bezpiecznego przechowywania notatek. Dostęp do notatek wymaga każdorazowo zalogowania z użyciem hasła. Aplikacja jest przysotosowana do obsługi wielu użytkowników. 

### Wykorzystane mechanizmy bezpieczeństwa

1. Maksymalnie 5 prób logowania na 1 minutę. Zabezpiecza to przed atakami brute-force.
2. Hasło użytkownika jest haszowane z wykorzystniem losowej soli oraz funkcji key-stretching która wynokuje 1000 iteracji.
3. Klucz do szyfrowania notatek algorymtm AES-256 jest generowany z hasła. Program wykorzystuje w tym celu PBKDF2 z HMAC-256. Liczba iteracji key-strethingu to 65536. 
4. Szyforwanie notatni odbywa się w trybie CBC z losowym wektorem początkowym. 
5. Przy każdej edycji/usunięciu/dodaniu notatni wszystkie dane zostają przeszyfrowane z wykorzystniem nowej soli oraz nowego wektora początkowego. Wpływa to co prawda na płynność działania aplikacji, lecz jej głównym celem jest zastosowanie mechanizmów krytpograficznych. 

### Zrzuty ekranu z działania aplikacji

<img src="screenshots/register.png" alt="Register screen" style="max-width: 200px;" >

<img src="screenshots/main_screen.png" alt="Main screen" style="max-width: 40%;" >

<img src="screenshots/create_note.png" alt="Create note" style="max-width: 40%;" >

<img src="screenshots/change_password.png" alt="Change password" style="max-width: 40%;" >

<img src="screenshots/delete_note.png" alt="Delete note" style="max-width: 40%;" >






