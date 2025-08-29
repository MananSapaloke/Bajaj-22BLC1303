WITH department_stats AS (
    SELECT 
        department_id,
        COUNT(*) as employee_count,
        AVG(salary) as avg_salary,
        MAX(salary) as max_salary
    FROM employees
    GROUP BY department_id
)
SELECT 
    d.department_name,
    ds.employee_count,
    ROUND(ds.avg_salary, 2) as average_salary,
    ds.max_salary,
    CASE 
        WHEN ds.avg_salary > 50000 THEN 'High Paying'
        WHEN ds.avg_salary > 30000 THEN 'Medium Paying'
        ELSE 'Low Paying'
    END as salary_category
FROM department_stats ds
JOIN departments d ON ds.department_id = d.department_id
ORDER BY ds.avg_salary DESC;
