variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "ap-south-1" # Mumbai (or change to us-east-1)
}

variable "instance_type" {
  description = "EC2 instance type (Free Tier eligible)"
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  description = "Name of the existing AWS Key Pair for SSH access"
  type        = string
  default     = "hospital-platform-key"
}

variable "allowed_ssh_ip" {
  description = "Your IP address with /32 CIDR for secure SSH access (e.g. 103.21.12.34/32)"
  type        = string
  default     = "0.0.0.0/0" # In production, restrict to your specific IP
}

variable "environment" {
  description = "Deployment environment name"
  type        = string
  default     = "production"
}
