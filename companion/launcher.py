"""Mango Goat Napper companion: connect to a creator and expose a local authenticated feed."""
import asyncio
import json
import logging
import os
from pathlib import Path
import secrets
import threading
import time
import tkinter as tk
from tkinter import messagebox
from aiohttp import web
from core import normalize
from service import Service
from settings import cooldown_seconds, load_settings, save_settings


def main():
    folder = Path(os.environ.get('LOCALAPPDATA', str(Path.home()))) / 'MangoGoatNapper'
    folder.mkdir(parents=True, exist_ok=True)
    logging.basicConfig(filename=folder / 'companion.log', level=logging.WARNING)
    token = secrets.token_urlsafe(32)
    settings_path = folder / 'settings.json'
    service = Service(token, load_settings(settings_path))
    loop = asyncio.new_event_loop()
    ready = threading.Event()
    errors = []
    stop = None

    async def serve():
        nonlocal stop
        stop = asyncio.Event()
        runner = web.AppRunner(service.app())
        try:
            await runner.setup()
            await web.TCPSite(runner, '127.0.0.1', 18765).start()
            (folder / 'connection.json').write_text(json.dumps({'token': token}), encoding='utf-8')
            ready.set()
            await stop.wait()
        except Exception as exc:
            errors.append(str(exc))
            ready.set()
        finally:
            await runner.cleanup()

    thread = threading.Thread(target=lambda: loop.run_until_complete(serve()), daemon=True)
    thread.start()
    root = tk.Tk()
    root.title('Mango Goat Napper Companion')
    root.geometry('500x380')
    root.resizable(False, False)
    tk.Label(root, text='Mango Goat Napper', font=('Segoe UI', 22, 'bold')).pack(pady=(12, 2))
    tk.Label(root, text='TikTok creator username or LIVE URL').pack()
    username = tk.Entry(root, width=42)
    username.pack(pady=5)
    row = tk.Frame(root)
    row.pack()
    async def connect(name):
        await service.stop()
        from pet_gifts import PetGifts
        service.feed = PetGifts()
        service.username = name
        if name:
            service.task = asyncio.create_task(service.watch())
    def submit():
        try:
            name = normalize(username.get())
        except ValueError as exc:
            messagebox.showerror('Invalid username', str(exc))
            return
        asyncio.run_coroutine_threadsafe(connect(name), loop)
    tk.Button(row, text='Connect', command=submit, width=12).pack(side='left', padx=5)
    tk.Button(row, text='Disconnect', command=lambda: asyncio.run_coroutine_threadsafe(connect(''), loop), width=12).pack(side='left', padx=5)
    cooldown_row = tk.Frame(root)
    cooldown_row.pack(pady=(8, 0))
    tk.Label(cooldown_row, text='Cooldown:').pack(side='left')
    minutes = tk.Entry(cooldown_row, width=4)
    minutes.insert(0, str(service.cooldown_seconds // 60))
    minutes.pack(side='left', padx=(6, 2))
    tk.Label(cooldown_row, text='min').pack(side='left')
    seconds = tk.Entry(cooldown_row, width=3)
    seconds.insert(0, str(service.cooldown_seconds % 60))
    seconds.pack(side='left', padx=(6, 2))
    tk.Label(cooldown_row, text='sec').pack(side='left')
    def save_cooldown():
        try:
            value = cooldown_seconds(minutes.get(), seconds.get())
            save_settings(settings_path, value)
        except (ValueError, OSError) as exc:
            messagebox.showerror('Cooldown', str(exc))
            return
        service.cooldown_seconds = value
        messagebox.showinfo('Cooldown', 'Saved. The new cooldown applies to the next goat.')
    tk.Button(cooldown_row, text='Save', command=save_cooldown).pack(side='left', padx=8)
    async def send_test():
        service.feed.test()
    tk.Button(root, text='Test goat (no gift needed)', command=lambda: asyncio.run_coroutine_threadsafe(send_test(), loop)).pack(pady=(8, 0))
    label = tk.Label(root, text='Starting…', wraplength=430)
    label.pack(pady=12)
    tk.Label(root, text='Keep this window open while playing.').pack()
    def refresh():
        if errors:
            messagebox.showerror('Mango Goat Napper', 'Cannot start local service. ' + errors[0])
            close()
            return
        game = 'Minecraft connected' if service.last_poll is not None and time.monotonic() - service.last_poll < 6 else 'Waiting for Minecraft mod'
        label.config(text=f'{service.status}  @{service.username}\n{service.message}\n{game}\n{service.feed.last_result}')
        root.after(1000, refresh)
    def close():
        if stop is not None and loop.is_running():
            loop.call_soon_threadsafe(stop.set)
        root.destroy()
    root.protocol('WM_DELETE_WINDOW', close)
    refresh()
    root.mainloop()
    thread.join(timeout=5)

if __name__ == '__main__':
    main()
