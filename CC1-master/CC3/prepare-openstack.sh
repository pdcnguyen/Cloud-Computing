#!/bin/bash

# create security group
openstack security group create open-all

# allow traffic
openstack security group rule create --protocol tcp --egress open-all
openstack security group rule create --protocol tcp --ingress open-all

openstack security group rule create --protocol udp --egress open-all
openstack security group rule create --protocol udp --ingress open-all

openstack security group rule create --protocol icmp --egress open-all
openstack security group rule create --protocol icmp --ingress open-all

# generate keys and send the private part to controller
openstack keypair create openstack-key > openstack-key
scp openstack-key phamdangcaonguyen@34.159.19.97:~/.ssh/

# create vm
openstack server create --flavor 2 --image c7f6b979-fe0e-4f37-b478-3adfedd747dd --key-name openstack-key --security-group open-all --nic net-id=4cdff2b8-a172-434b-82a6-2dd513cf955e OpenStack