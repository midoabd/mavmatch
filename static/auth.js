const form = document.querySelector('#auth-form');
const error = document.querySelector('#auth-error');
const csrf = () => document.cookie.split('; ').find(c => c.startsWith('csrftoken='))?.split('=')[1] || '';
async function submit(data, mode, button) {
  error.hidden = true;
  document.querySelectorAll('button').forEach(b => b.disabled = true);
  const original = button.innerHTML;
  button.textContent = 'One moment…';
  try {
    const response = await fetch(`/api/${mode}/`, {method:'POST', headers:{'Content-Type':'application/json','X-CSRFToken':csrf()}, body:JSON.stringify(data)});
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || 'Unable to continue. Please try again.');
    window.location.assign(result.next);
  } catch (e) {
    error.textContent = e instanceof TypeError ? 'Could not reach MavMatch. Check that the local server is running and try again.' : e.message;
    error.hidden = false;
    document.querySelectorAll('button').forEach(b => b.disabled = false);
    button.innerHTML = original;
  }
}
form.addEventListener('submit', e => {e.preventDefault(); submit(Object.fromEntries(new FormData(form)), form.dataset.mode, form.querySelector('button'));});
document.querySelector('#demo-login').addEventListener('click', e => submit({email:'demo@mavs.uta.edu',password:'MavMatchDemo!2026'}, 'login', e.currentTarget));
