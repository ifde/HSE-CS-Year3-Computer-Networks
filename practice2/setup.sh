sudo apt update

sudo apt install xrdp xfce4 xfce4-goodies xorgxrdp wireshark maven snapd -y
sudo snap install intellij-idea-community --classic
wget https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.deb && sudo apt install -f ./jdk-25_linux-x64_bin.deb

sudo adduser xrdp ssl-cert
sudo systemctl enable xrdp
sudo systemctl start xrdp

sudo sed -i 's/^\s*#\?PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config.d/50-cloud-init.conf
sudo systemctl restart ssh

sudo ufw allow 3389
sudo ufw allow ssh
sudo ufw enable
sudo ufw reload

sudo adduser student
sudo usermod -aG root student
sudo usermod -aG sudo student
sudo usermod -aG wireshark student
echo "student ALL=(ALL:ALL) ALL" | sudo tee -a /etc/sudoers

sudo su student
echo "xfce4-session" > ~/.xsession
exit

# lesson 3
sudo apt install -y libpcap-dev nethogs iperf3 iptraf-ng
