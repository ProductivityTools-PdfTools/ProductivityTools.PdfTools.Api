## Jenkins

Enable jenkins to run commands without sudo

```bash
sudo visudo
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop pdftools
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl start pdftools
```
