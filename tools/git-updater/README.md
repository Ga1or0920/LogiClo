# Git One-Button Update Tool

This mini toolkit lets you run `git add`, `git commit`, and `git push` with a single click from Windows. It lives outside the application sources so you can keep it separate from your main project code.

## Files

- `git-update.ps1` — PowerShell script that stages changes, asks for/creates a commit message, commits, and pushes to the current upstream.
- `git-update.bat` — Optional helper so you can double-click from Explorer or pin it to the taskbar. It simply invokes the PowerShell script with the proper execution policy.

## Quick Start

1. Copy the `tools/git-updater` folder to any location outside your project (e.g. `C:\Tools\git-updater`).
2. Open PowerShell and run:
   ```powershell
   Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
   ```
   (You only need to do this once so the script can run.)
3. Execute the script from anywhere:
   ```powershell
   C:\Tools\git-updater\git-update.ps1 -RepositoryPath "C:\Team\MyApplication"
   ```
   If you omit `-RepositoryPath`, the script assumes the folder that contains the script is the repository root.
4. Optionally create a desktop shortcut that points to `git-update.bat` for true one-button updates.

## How It Works

1. Verifies that Git is installed and the target directory is a Git repository.
2. Displays the current status so you can confirm the changes.
3. Prompts for a commit message (defaults to a timestamped message if you just press Enter).
4. Runs `git add -A`, `git commit`, and `git push` sequentially.
5. Restores your original working directory before exiting.

## Customising

- Pass `-RepositoryPath` when your script lives outside the repo.
- Provide `-CommitMessage "your message"` to skip the prompt.
- Add extra logic (e.g. run tests) by editing `git-update.ps1` in the indicated section.

## Safety Notes

- The script deliberately stops if there are no changes to commit.
- Push uses the current branch’s default upstream. Make sure your local branch tracks the correct remote branch (`git push -u origin main` once if needed).
- If `git commit` fails (e.g. merge conflict, lint hook), the script aborts without running `git push`.
