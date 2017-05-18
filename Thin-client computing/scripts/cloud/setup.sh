chdir ~

sudo apt-get install tightvncserver
sudo apt-get update

vncpasswd
# input password

cat > .vnc/xstartup << EOF
#!/bin/sh
xrdb $HOME/.Xresources
xsetroot -solid grey
export XKL_XMODMAP_DISABLE=1
/etc/X11/Xsession
if [ "$VNCDESKTOP" = "openoffice" ]; then
  mkdir ~/.config/libreoffice
  libreoffice
fi
EOF

chmod 760 .vnc/xstartup

git clone https://github.com/kanaka/noVNC.git

sudo apt-get install xserver-xorg-core xserver-xorg-input-all \
xserver-xorg-video-fbdev libx11-6 x11-common \
x11-utils x11-xkb-utils x11-xserver-utils xterm lightdm openbox

# gnome
sudo apt-get install aptitude tasksel
sudo tasksel install gnome-desktop --new-install

sudo apt-get install default-jre

mkdir openoffice
cd openoffice
wget http://netassist.dl.sourceforge.net/project/openofficeorg.mirror/4.1.2/binaries/en-US/Apache_OpenOffice_4.1.2_Linux_x86-64_install-deb_en-US.tar.gz
tar xzf Apache_OpenOffice_4.1.2_Linux_x86-64_install-deb_en-US.tar.gz

cd en-US/DEBS/
sudo dpkg -i *.deb
cd desktop-integration/
sudo dpkg -i openoffice4.1-debian-menus*.deb
cd ~

