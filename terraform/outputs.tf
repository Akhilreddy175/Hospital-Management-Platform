output "ec2_public_ip" {
  description = "Public IP address of the deployed EC2 server"
  value       = aws_instance.hospital_server.public_ip
}

output "app_url" {
  description = "Application HTTPS URL"
  value       = "https://${aws_instance.hospital_server.public_ip}"
}

output "health_endpoint" {
  description = "Spring Boot Actuator Health Probe"
  value       = "https://${aws_instance.hospital_server.public_ip}/actuator/health"
}

output "ssh_command" {
  description = "Command to SSH into the EC2 instance"
  value       = "ssh -i ${var.key_name}.pem ubuntu@${aws_instance.hospital_server.public_ip}"
}
