# ==========================================
# AWS Security Group (Least Privilege Network Policy)
# ==========================================

resource "aws_security_group" "hospital_sg" {
  name        = "hospital-platform-sg-${var.environment}"
  description = "Security group for Hospital Appointment Platform (Ports 22, 80, 443 only)"

  # Port 22: SSH (Restricted to administrator IP)
  ingress {
    description = "SSH Access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_ip]
  }

  # Port 80: HTTP (Public redirect to HTTPS)
  ingress {
    description = "HTTP Traffic (Nginx Redirect)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Port 443: HTTPS (Public TLS Traffic)
  ingress {
    description = "HTTPS Traffic (TLS Terminated at Nginx)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Outbound: Allow all egress traffic for updates, container pulling
  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "hospital-platform-sg"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
