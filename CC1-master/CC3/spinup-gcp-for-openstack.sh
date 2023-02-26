#!/bin/bash

# 1) create two additional VPC networks
gcloud compute networks create cc-network1 \
    --subnet-mode=custom 

gcloud compute networks create cc-network2 \
    --subnet-mode=custom

# 2) create subnets
gcloud compute networks subnets create cc-subnet1 \
    --network=cc-network1 \
    --range=10.0.0.0/8 \
    --secondary-range=subnet-1-secondary=192.168.0.0/16 \
    --region=europe-west3

gcloud compute networks subnets create cc-subnet2 \
    --network=cc-network2 \
    --range=172.16.0.0/12 \
    --region=europe-west3

 # 4) create disk
 gcloud compute disks create cc-disk-ass3 \
    --zone=europe-west3-a \
    --size=100GB \
    --image-project=ubuntu-os-cloud \
    --image-family=ubuntu-2004-lts

# 5) create custom image with special licence key for nested virtualization
gcloud compute images create cc-img-ass3 \
	--source-disk cc-disk-ass3 \
	--source-disk-zone europe-west3-a \
	--licenses "https://www.googleapis.com/compute/v1/projects/vm-options/global/licenses/enable-vmx" 

# 6) start 3 VMs

# 6a) controller
gcloud compute instances create controller \
    --zone=europe-west3-a \
    --min-cpu-platform "Intel Ice Lake" \
    --image cc-img-ass3 \
    --tags=cc \
    --machine-type=n2-standard-2 \
    --network-interface network=cc-network1,subnet=cc-subnet1 \
    --network-interface network=cc-network2,subnet=cc-subnet2 

# 6b) compute1
gcloud compute instances create compute1 \
    --zone=europe-west3-a \
    --min-cpu-platform "Intel Ice Lake" \
    --image cc-img-ass3 \
    --tags=cc \
    --machine-type=n2-standard-2 \
    --network-interface network=cc-network1,subnet=cc-subnet1 \
    --network-interface network=cc-network2,subnet=cc-subnet2

# 6c) compute2
gcloud compute instances create compute2 \
    --zone=europe-west3-a \
    --min-cpu-platform "Intel Ice Lake" \
    --image cc-img-ass3 \
    --tags=cc \
    --machine-type=n2-standard-2 \
    --network-interface network=cc-network1,subnet=cc-subnet1 \
    --network-interface network=cc-network2,subnet=cc-subnet2

# 7) create firewall rule for TCP, UDP, ICMP
# allow ICMP traffic
gcloud compute firewall-rules create cc-ass3-firewall-network1 \
    --action=ALLOW \
    --network=cc-network1 \
    --rules=icmp,tcp,udp \
    --source-ranges=10.0.0.0/8,192.168.0.0/16,172.16.0.0/12 \
    --destination-ranges=10.0.0.0/8,192.168.0.0/16,172.16.0.0/12 \
    --target-tags=cc

gcloud compute firewall-rules create cc-ass3-firewall-network2 \
    --action=ALLOW \
    --network=cc-network2 \
    --rules=icmp,tcp,udp \
    --source-ranges=10.0.0.0/8,192.168.0.0/16,172.16.0.0/12 \
    --destination-ranges=10.0.0.0/8,192.168.0.0/16,172.16.0.0/12 \
    --target-tags=cc

# 8) open required OpenStack ports
gcloud compute firewall-rules create cc-ass3-firewall-open-stack \
    --action=ALLOW \
    --rules=tcp,icmp \
    --network=cc-network1 \
    --target-tags=cc


