## Jenkins

Enable jenkins to run commands without sudo

```bash
sudo visudo
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop pdftools
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl start pdftools
jenkins ALL=(ALL) NOPASSWD: /usr/bin/cp deploy/pdftools.service /etc/systemd/system/pdftools.service
jenkins ALL=(ALL) NOPASSWD: /usr/bin/cp /var/lib/jenkins/workspace/PT.PDFTools/deploy/pdftools.service /etc/systemd/system/pdftools.service
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl daemon-reload
jenkins ALL=(ALL) NOPASSWD: /usr/bin/systemctl enable pdftools
```

Env variables

```
sudo touch /etc/default/pdftools
sudo nano /etc/default/pdftools
```

```
SERVER_PORT=8080
```