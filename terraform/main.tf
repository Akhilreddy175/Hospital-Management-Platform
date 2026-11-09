terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Fetch the latest Ubuntu 22.04 LTS AMI
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# EC2 Instance Provisioning
resource "aws_instance" "hospital_server" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.hospital_sg.id]

  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    delete_on_termination = true
  }

  # Automated Provisioning Bootstrap Script (Docker + Compose + Git)
  user_data = <<-EOF
              #!/bin/bash
              set -e

              # Update system packages
              apt-get update -y
              apt-get upgrade -y

              # Install prerequisites
              apt-get install -y ca-certificates curl gnupg lsb-release git

              # Install Docker
              mkdir -p /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
              
              apt-get update -y
              apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

              # Enable Docker for ubuntu user
              usermod -aG docker ubuntu
              systemctl enable docker
              systemctl start docker

              # Create application directory
              mkdir -p /home/ubuntu/hospital-appointment-platform
              chown -R ubuntu:ubuntu /home/ubuntu/hospital-appointment-platform
              EOF

  tags = {
    Name        = "hospital-appointment-server"
    Role        = "DevOps & Cloud Engineering Assessment"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
